package ar.utn.donatrack.logistica.rutas.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Una ejecución del planificador externo (un lote de hasta 100 entregas). */
public class SolicitudPlanificacion {

    public enum Estado { ENVIADA, COMPLETADA, FALLIDA }

    private final UUID id;
    private final LocalDate fechaRutas;
    private final List<UUID> entregaIds;
    private Estado estado;
    private final Instant enviadaEn;
    private Instant completadaEn;
    private int rutasGeneradas;
    private int sinAsignar;

    public SolicitudPlanificacion(UUID id, LocalDate fechaRutas, List<UUID> entregaIds, Estado estado, Instant enviadaEn,
                                  Instant completadaEn, int rutasGeneradas, int sinAsignar) {
        this.id = Objects.requireNonNull(id);
        this.fechaRutas = fechaRutas;
        this.entregaIds = List.copyOf(entregaIds);
        this.estado = estado;
        this.enviadaEn = enviadaEn;
        this.completadaEn = completadaEn;
        this.rutasGeneradas = rutasGeneradas;
        this.sinAsignar = sinAsignar;
    }

    public static SolicitudPlanificacion nueva(UUID id, LocalDate fechaRutas, List<UUID> entregaIds, Instant ahora) {
        return new SolicitudPlanificacion(id, fechaRutas, entregaIds, Estado.ENVIADA, ahora, null, 0, 0);
    }

    public boolean pendiente() {
        return estado == Estado.ENVIADA;
    }

    public void completar(int rutas, int sinAsignar, Instant ahora) {
        this.estado = Estado.COMPLETADA;
        this.rutasGeneradas = rutas;
        this.sinAsignar = sinAsignar;
        this.completadaEn = ahora;
    }

    public void fallar(Instant ahora) {
        this.estado = Estado.FALLIDA;
        this.completadaEn = ahora;
    }

    public UUID id() { return id; }
    public LocalDate fechaRutas() { return fechaRutas; }
    public List<UUID> entregaIds() { return entregaIds; }
    public Estado estado() { return estado; }
    public Instant enviadaEn() { return enviadaEn; }
    public Instant completadaEn() { return completadaEn; }
    public int rutasGeneradas() { return rutasGeneradas; }
    public int sinAsignar() { return sinAsignar; }
}
