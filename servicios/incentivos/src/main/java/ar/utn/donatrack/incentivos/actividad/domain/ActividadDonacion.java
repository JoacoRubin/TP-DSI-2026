package ar.utn.donatrack.incentivos.actividad.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Proyección local de una donación (alimentada por eventos de Donaciones). Incentivos no
 * consulta a Donaciones: mantiene su propio modelo de lectura, optimizado para analítica.
 */
public class ActividadDonacion {

    private final UUID donacionId;
    private final UUID donanteId;
    private final UUID cargaId;
    private final String categoria;
    private final String subcategoria;
    private final int cantidad;
    private final Instant registradaEn;
    private boolean entregada;
    private UUID entidadId;
    private Instant entregadaEn;

    public ActividadDonacion(UUID donacionId, UUID donanteId, UUID cargaId, String categoria, String subcategoria,
                             int cantidad, Instant registradaEn, boolean entregada, UUID entidadId, Instant entregadaEn) {
        this.donacionId = Objects.requireNonNull(donacionId);
        this.donanteId = Objects.requireNonNull(donanteId);
        this.cargaId = cargaId;
        this.categoria = categoria;
        this.subcategoria = subcategoria;
        this.cantidad = cantidad;
        this.registradaEn = registradaEn;
        this.entregada = entregada;
        this.entidadId = entidadId;
        this.entregadaEn = entregadaEn;
    }

    public static ActividadDonacion registrada(UUID donacionId, UUID donanteId, UUID cargaId, String categoria,
                                               String subcategoria, int cantidad, Instant registradaEn) {
        return new ActividadDonacion(donacionId, donanteId, cargaId, categoria, subcategoria, cantidad, registradaEn,
                false, null, null);
    }

    public void marcarEntregada(UUID entidadId, Instant fecha) {
        this.entregada = true;
        this.entidadId = entidadId;
        this.entregadaEn = fecha;
    }

    public UUID donacionId() { return donacionId; }
    public UUID donanteId() { return donanteId; }
    public UUID cargaId() { return cargaId; }
    public String categoria() { return categoria; }
    public String subcategoria() { return subcategoria; }
    public int cantidad() { return cantidad; }
    public Instant registradaEn() { return registradaEn; }
    public boolean entregada() { return entregada; }
    public UUID entidadId() { return entidadId; }
    public Instant entregadaEn() { return entregadaEn; }
}
