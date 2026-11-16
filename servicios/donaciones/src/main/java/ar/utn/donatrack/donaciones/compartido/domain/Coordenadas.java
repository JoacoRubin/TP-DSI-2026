package ar.utn.donatrack.donaciones.compartido.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

public record Coordenadas(double latitud, double longitud) {
    public Coordenadas {
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            throw new ReglaDeNegocioException("Coordenadas fuera de rango");
        }
    }
}
