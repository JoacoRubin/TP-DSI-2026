package ar.utn.donatrack.logistica.entregas.domain;

/** Copia desnormalizada de lo que se transporta (para comprobantes y mapas). */
public record DetalleDonacion(String descripcion, String subcategoria, int cantidad, String unidad) {
}
