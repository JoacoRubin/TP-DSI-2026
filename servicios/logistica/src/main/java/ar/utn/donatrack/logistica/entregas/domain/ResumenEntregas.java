package ar.utn.donatrack.logistica.entregas.domain;

/** Totales REALES de un período (agregación), para métricas públicas: no una muestra. */
public record ResumenEntregas(long entregadas, long entidades) {
}
