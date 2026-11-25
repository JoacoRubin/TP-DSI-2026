package ar.utn.donatrack.logistica.rutas.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.logistica.compartido.application.PublicadorEventos;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.flota.domain.CamionRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.Parada;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import ar.utn.donatrack.logistica.rutas.domain.RutaRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacion;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacionRepositorio;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Callback del planificador externo. Registra las rutas generadas, marca las entregas
 * planificadas y devuelve a "Pendiente" las que no pudieron asignarse (se replanifican en la
 * próxima ejecución). Idempotente: un callback repetido se ignora.
 */
public class RegistrarResultadoPlanificacion {

    public record ParadaPropuesta(int orden, List<UUID> entregaIds) {
    }

    public record RutaPropuesta(UUID camionId, List<ParadaPropuesta> paradas) {
    }

    public record Resultado(UUID solicitudId, List<RutaPropuesta> rutas, List<UUID> sinAsignar) {
    }

    private final SolicitudPlanificacionRepositorio solicitudes;
    private final EntregaRepositorio entregas;
    private final CamionRepositorio camiones;
    private final RutaRepositorio rutas;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public RegistrarResultadoPlanificacion(SolicitudPlanificacionRepositorio solicitudes, EntregaRepositorio entregas,
                                           CamionRepositorio camiones, RutaRepositorio rutas,
                                           PublicadorEventos eventos, Clock reloj) {
        this.solicitudes = solicitudes;
        this.entregas = entregas;
        this.camiones = camiones;
        this.rutas = rutas;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public List<Ruta> ejecutar(Resultado resultado) {
        SolicitudPlanificacion s = solicitudes.buscar(resultado.solicitudId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de planificación", resultado.solicitudId()));
        if (!s.pendiente()) {
            return List.of();
        }
        Instant ahora = reloj.instant();
        Map<UUID, Entrega> delLote = entregas.buscarTodas(s.entregaIds()).stream()
                .collect(Collectors.toMap(Entrega::id, Function.identity()));
        List<Ruta> generadas = new ArrayList<>();
        List<EventosLogistica.EntregaPlanificada> planificadas = new ArrayList<>();

        for (RutaPropuesta rp : resultado.rutas() == null ? List.<RutaPropuesta>of() : resultado.rutas()) {
            Camion camion = camiones.buscar(rp.camionId()).orElse(null);
            if (camion == null) {
                continue;
            }
            List<Parada> paradas = new ArrayList<>();
            List<Entrega> incluidas = new ArrayList<>();
            rp.paradas().stream().sorted(Comparator.comparingInt(ParadaPropuesta::orden)).forEach(pp -> {
                List<Entrega> es = pp.entregaIds().stream().map(delLote::get).filter(Objects::nonNull)
                        .filter(e -> e.estado() == EstadoEntrega.EN_PLANIFICACION).toList();
                if (!es.isEmpty()) {
                    paradas.add(Parada.de(paradas.size() + 1, es.get(0).entidadId(), es.get(0).destino(), es));
                    incluidas.addAll(es);
                }
            });
            if (paradas.isEmpty()) {
                continue;
            }
            Ruta ruta = Ruta.planificar(UUID.randomUUID(), s.fechaRutas(), camion, paradas, ahora);
            rutas.guardar(ruta);
            generadas.add(ruta);
            incluidas.forEach(e -> {
                e.planificar(ruta.id(), camion, ahora);
                planificadas.add(new EventosLogistica.EntregaPlanificada(e.id(), e.donacionId(), ruta.id(), camion.patente(), ahora));
            });
        }

        int sinAsignar = 0;
        for (Entrega e : delLote.values()) {
            if (e.estado() == EstadoEntrega.EN_PLANIFICACION) {
                e.devolverAPendiente("El planificador no pudo asignarla a una ruta: se replanificará", ahora);
                sinAsignar++;
            }
        }
        entregas.guardarTodas(delLote.values());
        s.completar(generadas.size(), sinAsignar, ahora);
        solicitudes.guardar(s);
        // Recién ahora, con todo guardado: si algo de lo anterior falla, Donaciones no se entera de una
        // planificación que Logística no tiene, y el callback (solicitud aún pendiente) se puede reintentar.
        planificadas.forEach(eventos::publicar);
        return generadas;
    }
}
