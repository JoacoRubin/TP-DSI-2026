package ar.utn.donatrack.donaciones.catalogo.domain;

import java.util.UUID;

/** Unidad mínima de asignación del sistema (ej.: "Fideos secos", "Sillas"). */
public record Subcategoria(UUID id, String nombre) {
}
