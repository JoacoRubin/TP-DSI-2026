package ar.utn.donatrack.autenticacion.usuarios.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

/** Política mínima de contraseñas: 8+ caracteres, al menos una letra y un número. */
public final class PoliticaClaves {

    public static final int LONGITUD_MINIMA = 8;

    private PoliticaClaves() {
    }

    public static void validar(String clave) {
        if (clave == null || clave.length() < LONGITUD_MINIMA
                || clave.chars().noneMatch(Character::isLetter)
                || clave.chars().noneMatch(Character::isDigit)) {
            throw new ReglaDeNegocioException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres, con letras y números");
        }
    }
}
