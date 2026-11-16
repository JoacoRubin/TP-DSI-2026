package ar.utn.donatrack.logistica.entregas.domain;

import java.time.Instant;

public record RegistroEstado(EstadoEntrega estado, Instant fecha, String detalle) {
}
