package ar.utn.donatrack.donaciones.donantes.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

public record Documento(TipoDocumento tipo, String numero) {

    public Documento {
        if (tipo == null || numero == null || numero.isBlank()) {
            throw new ReglaDeNegocioException("El documento requiere tipo y número");
        }
        if (tipo != TipoDocumento.PASAPORTE) {
            numero = numero.replaceAll("[^0-9]", "");
        } else {
            numero = numero.trim().toUpperCase();
        }
        boolean valido = switch (tipo) {
            case DNI, LE, LC -> numero.matches("\\d{6,9}");
            case CUIT, CUIL -> numero.matches("\\d{11}");
            case PASAPORTE -> numero.matches("[A-Z0-9]{5,15}");
        };
        if (!valido) {
            throw new ReglaDeNegocioException("Número de " + tipo + " inválido: " + numero);
        }
    }
}
