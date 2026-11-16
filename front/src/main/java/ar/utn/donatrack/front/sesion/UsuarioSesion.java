package ar.utn.donatrack.front.sesion;

import java.io.Serial;
import java.io.Serializable;

/**
 * Principal guardado en la sesión HTTP del servidor. Incluye el JWT emitido por Autenticación,
 * que se usa para llamar a las APIs en nombre del usuario.
 */
public record UsuarioSesion(String id, String email, String nombre, String rol, String perfil, String referencia,
                            String token) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public boolean esDonanteHumano() {
        return "DONANTE".equals(rol) && "HUMANA".equals(perfil);
    }

    public String inicial() {
        return nombre == null || nombre.isBlank() ? "?" : nombre.substring(0, 1).toUpperCase();
    }

    @Override
    public String toString() {
        return email; // nunca loguear el token
    }
}
