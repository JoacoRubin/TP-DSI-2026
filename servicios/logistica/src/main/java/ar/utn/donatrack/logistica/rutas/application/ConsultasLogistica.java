package ar.utn.donatrack.logistica.rutas.application;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.rutas.domain.EstadoRuta;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import ar.utn.donatrack.logistica.rutas.domain.RutaRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacion;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacionRepositorio;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class ConsultasLogistica {

    private final RutaRepositorio rutas;
    private final EntregaRepositorio entregas;
    private final SolicitudPlanificacionRepositorio solicitudes;
    private final Clock reloj;

    public ConsultasLogistica(RutaRepositorio rutas, EntregaRepositorio entregas,
                              SolicitudPlanificacionRepositorio solicitudes, Clock reloj) {
        this.rutas = rutas;
        this.entregas = entregas;
        this.solicitudes = solicitudes;
        this.reloj = reloj;
    }

    /** Seguimiento en tiempo real de las entregas activas que le interesan a quien consulta. */
    public List<Ruta> seguimiento(Actor actor) {
        UUID ref = actor.referencia() == null ? null : UUID.fromString(actor.referencia());
        return switch (actor.rol()) {
            case "DONANTE" -> rutas.activasDeDonante(ref);
            case "BENEFICIARIA" -> rutas.activasDeEntidad(ref);
            case "ADMIN", "SERVICIO" -> rutas.enEstados(List.of(EstadoRuta.EN_CURSO));
            default -> throw new OperacionNoPermitidaException("Sin acceso al seguimiento");
        };
    }

    /** Dashboard administrativo: rutas en curso y planificadas. */
    public List<Ruta> monitoreo() {
        return rutas.enEstados(List.of(EstadoRuta.EN_CURSO, EstadoRuta.PLANIFICADA));
    }

    public List<Ruta> rutasDeChofer(String patente) {
        return rutas.dePatente(patente, List.of(EstadoRuta.PLANIFICADA, EstadoRuta.EN_CURSO));
    }

    public List<Ruta> rutasRecientes() {
        return rutas.recientes(50);
    }

    public Ruta ruta(UUID id) {
        return rutas.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Ruta", id));
    }

    public List<Entrega> entregasDeRuta(UUID rutaId) {
        return entregas.deRuta(rutaId);
    }

    public List<Entrega> entregas(EstadoEntrega estado, UUID entidadId, UUID donanteId) {
        return entregas.filtrar(estado, entidadId, donanteId);
    }

    public Entrega entrega(UUID id) {
        return entregas.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Entrega", id));
    }

    /** Público (sin login): entregas realizadas recientemente, para landing, galería y mapa. */
    public List<Entrega> entregadasRecientes(int dias, int limite) {
        return entregas.entregadasDesde(reloj.instant().minus(Duration.ofDays(dias)), limite);
    }

    public List<SolicitudPlanificacion> solicitudesRecientes() {
        return solicitudes.recientes(20);
    }
}
