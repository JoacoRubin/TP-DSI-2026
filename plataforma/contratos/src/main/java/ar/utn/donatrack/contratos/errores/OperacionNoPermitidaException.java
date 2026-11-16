package ar.utn.donatrack.contratos.errores;

/** El actor está autenticado pero no puede operar sobre ese recurso. Traducida a HTTP 403. */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
