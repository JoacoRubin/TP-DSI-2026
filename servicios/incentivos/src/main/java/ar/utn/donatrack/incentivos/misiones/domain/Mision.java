package ar.utn.donatrack.incentivos.misiones.domain;

/**
 * @param objetivo  valor a alcanzar (inclusive)
 * @param insignia  nombre de la insignia que otorga
 * @param icono     emoji/ícono de la insignia
 */
public record Mision(String id, CategoriaDonante categoria, int orden, String nombre, String descripcion,
                     TipoMision tipo, int objetivo, String insignia, String icono) {
}
