package ar.utn.donatrack.autenticacion.usuarios.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Los tokens de activación son secretos aleatorios de alta entropía: alcanza con SHA-256
 * (no hace falta BCrypt, que está pensado para contraseñas elegidas por humanos).
 * En la base sólo se guarda el hash.
 */
final class TokensActivacion {

    static final Duration VIGENCIA = Duration.ofDays(7);

    private TokensActivacion() {
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
