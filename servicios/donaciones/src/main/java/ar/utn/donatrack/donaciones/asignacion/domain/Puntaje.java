package ar.utn.donatrack.donaciones.asignacion.domain;

import java.util.UUID;

public record Puntaje(UUID necesidadId, UUID entidadId, double valor) {
}
