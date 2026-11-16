package ar.utn.donatrack.autenticacion.usuarios.application;

import java.util.UUID;

public record Sesion(TokenEmitido token, UUID usuarioId, String email, String nombre, String rol, String perfil,
                     String referencia) {
}
