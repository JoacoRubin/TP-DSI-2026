package ar.utn.donatrack.logistica.entregas.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

/** Copia desnormalizada de la entidad beneficiaria: Logística no consulta a Donaciones. */
public record Destino(String entidadNombre, String direccion, double latitud, double longitud) {
    public Destino {
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            throw new ReglaDeNegocioException("Coordenadas del destino inválidas");
        }
    }
}
