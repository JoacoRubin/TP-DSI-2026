package ar.utn.donatrack.autenticacion.usuarios.application;

import java.util.UUID;

/**
 * @param tokenActivacion token en claro (sólo se devuelve una vez, para que el servicio
 *                        dueño del contacto envíe el enlace). Null si no aplica.
 */
public record ResultadoAlta(UUID usuarioId, String email, boolean creado, String tokenActivacion) {
}
