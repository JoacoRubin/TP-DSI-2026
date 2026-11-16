package ar.utn.donatrack.donaciones.compartido.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

public final class Textos {
    private Textos() {
    }

    public static String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDeNegocioException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }

    public static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
