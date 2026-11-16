package ar.utn.donatrack.donaciones.donaciones.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Bien tal como lo declara la persona administradora en la carga única, ANTES de segmentar.
 *
 * @param pesoKg    estimación opcional (para planificar la carga de los camiones)
 * @param volumenM3 estimación opcional
 */
public record BienDeclarado(String descripcion, UUID subcategoriaId, int cantidad, String unidad,
                            LocalDate fechaVencimiento, EstadoUso estadoUso, String fotoUrl,
                            Double pesoKg, Double volumenM3) {
}
