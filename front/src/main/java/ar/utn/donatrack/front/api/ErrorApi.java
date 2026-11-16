package ar.utn.donatrack.front.api;

import java.util.Map;

/** Error devuelto por un servicio (traducido desde su Problem Detail RFC 9457). */
public class ErrorApi extends RuntimeException {

    private final int estado;
    private final Map<String, String> errores;

    public ErrorApi(int estado, String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.estado = estado;
        this.errores = errores == null ? Map.of() : errores;
    }

    public int estado() {
        return estado;
    }

    public Map<String, String> errores() {
        return errores;
    }

    public boolean noAutenticado() {
        return estado == 401;
    }

    /** Mensaje para mostrar: detalle + errores de campos si los hay. */
    public String mensajeCompleto() {
        if (errores.isEmpty()) {
            return getMessage();
        }
        StringBuilder sb = new StringBuilder(getMessage() == null ? "Revisá los datos" : getMessage()).append(": ");
        errores.forEach((campo, msg) -> sb.append(campo).append(" ").append(msg).append("; "));
        return sb.toString();
    }
}
