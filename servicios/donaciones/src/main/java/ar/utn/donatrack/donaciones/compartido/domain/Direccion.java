package ar.utn.donatrack.donaciones.compartido.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

/** Value Object: dos direcciones con los mismos datos son la misma dirección. */
public record Direccion(String calle, String altura, String localidad, String provincia, String codigoPostal,
                        Coordenadas coordenadas) {

    public Direccion {
        if (calle == null || calle.isBlank() || localidad == null || localidad.isBlank()) {
            throw new ReglaDeNegocioException("La dirección requiere al menos calle y localidad");
        }
        calle = calle.trim();
        localidad = localidad.trim();
    }

    public String formateada() {
        StringBuilder sb = new StringBuilder(calle);
        if (altura != null && !altura.isBlank()) {
            sb.append(' ').append(altura.trim());
        }
        sb.append(", ").append(localidad);
        if (provincia != null && !provincia.isBlank()) {
            sb.append(", ").append(provincia.trim());
        }
        return sb.toString();
    }
}
