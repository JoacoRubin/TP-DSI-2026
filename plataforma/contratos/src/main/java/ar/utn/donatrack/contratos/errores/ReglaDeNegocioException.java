package ar.utn.donatrack.contratos.errores;

/**
 * Se lanza cuando una operación viola un invariante del dominio.
 * La capa web la traduce a HTTP 422 (Unprocessable Entity).
 */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
