package ar.utn.donatrack.contratos.errores;

/** Traducida a HTTP 404 por la capa web. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " no encontrado/a: " + id);
    }
}
