package ar.utn.donatrack.donaciones.donaciones.domain;

import java.util.UUID;

/** Criterios opcionales de búsqueda (null = sin filtrar). */
public record FiltroDonaciones(UUID donanteId, UUID entidadId, EstadoDonacion estado, String categoria,
                               UUID subcategoriaId) {

    public static FiltroDonaciones ninguno() {
        return new FiltroDonaciones(null, null, null, null, null);
    }
}
