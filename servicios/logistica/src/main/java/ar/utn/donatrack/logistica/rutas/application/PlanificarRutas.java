package ar.utn.donatrack.logistica.rutas.application;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.flota.domain.CamionRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.Lotes;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacion;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacionRepositorio;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Envía las entregas pendientes al planificador externo, en lotes de 100 (restricción del
 * proveedor). Las rutas resultantes llegan luego por callback.
 */
public class PlanificarRutas {

    private final EntregaRepositorio entregas;
    private final CamionRepositorio camiones;
    private final SolicitudPlanificacionRepositorio solicitudes;
    private final PlanificadorRutas planificador;
    private final PlanificadorRutas.Ubicacion deposito;
    private final Clock reloj;

    public PlanificarRutas(EntregaRepositorio entregas, CamionRepositorio camiones,
                           SolicitudPlanificacionRepositorio solicitudes, PlanificadorRutas planificador,
                           PlanificadorRutas.Ubicacion deposito, Clock reloj) {
        this.entregas = entregas;
        this.camiones = camiones;
        this.solicitudes = solicitudes;
        this.planificador = planificador;
        this.deposito = deposito;
        this.reloj = reloj;
    }

    /** @return cantidad de solicitudes (lotes) enviadas */
    public int ejecutar() {
        List<Entrega> pendientes = entregas.enEstado(EstadoEntrega.PENDIENTE);
        if (pendientes.isEmpty()) {
            return 0;
        }
        List<Camion> flota = camiones.activos();
        if (flota.isEmpty()) {
            throw new ReglaDeNegocioException("No hay camiones activos para planificar rutas");
        }
        List<PlanificadorRutas.CamionDisponible> disponibles = flota.stream()
                .map(c -> new PlanificadorRutas.CamionDisponible(c.id(), c.patente(), c.cargaKg(), c.volumenM3()))
                .toList();
        LocalDate fechaRutas = LocalDate.now(reloj).plusDays(1);
        int enviadas = 0;
        for (List<Entrega> lote : Lotes.partir(pendientes, Lotes.MAXIMO_PLANIFICADOR)) {
            Instant ahora = reloj.instant();
            SolicitudPlanificacion s = SolicitudPlanificacion.nueva(UUID.randomUUID(), fechaRutas,
                    lote.stream().map(Entrega::id).toList(), ahora);
            lote.forEach(e -> e.enviarAPlanificar(ahora));
            solicitudes.guardar(s);
            entregas.guardarTodas(lote);
            try {
                planificador.solicitar(new PlanificadorRutas.Pedido(s.id(), deposito, disponibles, lote.stream()
                        .map(e -> new PlanificadorRutas.EntregaAPlanificar(e.id(), e.entidadId(), e.destino().latitud(),
                                e.destino().longitud(), e.pesoKg(), e.volumenM3()))
                        .toList()));
                enviadas++;
            } catch (RuntimeException falla) {
                // Compensación: el lote vuelve a la cola y la solicitud queda registrada como fallida.
                lote.forEach(e -> e.devolverAPendiente("Planificador no disponible", reloj.instant()));
                entregas.guardarTodas(lote);
                s.fallar(reloj.instant());
                solicitudes.guardar(s);
                throw new IllegalStateException("El planificador de rutas no respondió: " + falla.getMessage(), falla);
            }
        }
        return enviadas;
    }
}
