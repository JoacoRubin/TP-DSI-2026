package ar.utn.donatrack.autenticacion.usuarios.application;

import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;

/** Puerto: emite tokens de acceso firmados (adaptador: JWT RS256). */
public interface EmisorTokens {

    TokenEmitido paraUsuario(Usuario usuario);

    TokenEmitido paraServicio(String clientId);
}
