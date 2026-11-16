package ar.utn.donatrack.donaciones.donaciones.domain;

import java.time.Instant;

/** Registro de auditoría inmutable: qué cambió, cuándo, quién y por qué. */
public record CambioEstado(EstadoDonacion desde, EstadoDonacion hasta, Instant fecha, String responsable, String motivo) {
}
