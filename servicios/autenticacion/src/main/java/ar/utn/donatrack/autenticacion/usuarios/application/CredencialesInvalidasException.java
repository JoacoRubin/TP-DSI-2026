package ar.utn.donatrack.autenticacion.usuarios.application;

/**
 * Mensaje deliberadamente genérico: no revela si el email existe (evita enumeración de usuarios).
 */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
