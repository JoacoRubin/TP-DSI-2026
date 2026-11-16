package ar.utn.donatrack.donaciones.donaciones.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.domain.Textos;

/** Bien físico dentro de una donación (ya segmentada). */
public record Bien(String descripcion, int cantidad, String fotoUrl) {
    public Bien {
        descripcion = Textos.requerido(descripcion, "descripción del bien");
        if (cantidad <= 0) {
            throw new ReglaDeNegocioException("La cantidad de '" + descripcion + "' debe ser mayor a cero");
        }
    }
}
