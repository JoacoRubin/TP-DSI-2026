package ar.utn.donatrack.autenticacion.usuarios.application;

import ar.utn.donatrack.autenticacion.usuarios.domain.Rol;

/** Comando de alta. Si {@code clave} es null, la cuenta queda pendiente de activación. */
public record NuevoUsuario(String email, String nombre, Rol rol, String perfil, String referencia, String clave) {
}
