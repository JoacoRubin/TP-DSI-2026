package ar.utn.donatrack.autenticacion.usuarios.infrastructure.seguridad;

import com.nimbusds.jose.jwk.RSAKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Par de claves RSA para firmar JWT. Se persiste en disco para que los tokens sobrevivan a un
 * reinicio en desarrollo. En producción la clave privada debe venir de un gestor de secretos.
 */
public final class ClavesRsa {

    private final RSAKey jwk;

    private ClavesRsa(RSAPublicKey publica, RSAPrivateKey privada) {
        try {
            byte[] huella = MessageDigest.getInstance("SHA-256").digest(publica.getEncoded());
            String kid = "donatrack-" + HexFormat.of().formatHex(huella).substring(0, 12);
            this.jwk = new RSAKey.Builder(publica).privateKey(privada).keyID(kid).build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static ClavesRsa cargarOGenerar(Path directorio) {
        try {
            Path privadaPath = directorio.resolve("jwt-privada.key");
            Path publicaPath = directorio.resolve("jwt-publica.key");
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            if (Files.exists(privadaPath) && Files.exists(publicaPath)) {
                RSAPrivateKey privada = (RSAPrivateKey) fabrica.generatePrivate(
                        new PKCS8EncodedKeySpec(Base64.getDecoder().decode(Files.readString(privadaPath).trim())));
                RSAPublicKey publica = (RSAPublicKey) fabrica.generatePublic(
                        new X509EncodedKeySpec(Base64.getDecoder().decode(Files.readString(publicaPath).trim())));
                return new ClavesRsa(publica, privada);
            }
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair par = generador.generateKeyPair();
            Files.createDirectories(directorio);
            Files.writeString(privadaPath, Base64.getEncoder().encodeToString(par.getPrivate().getEncoded()));
            Files.writeString(publicaPath, Base64.getEncoder().encodeToString(par.getPublic().getEncoded()));
            return new ClavesRsa((RSAPublicKey) par.getPublic(), (RSAPrivateKey) par.getPrivate());
        } catch (IOException | java.security.GeneralSecurityException e) {
            throw new IllegalStateException("No se pudieron cargar/generar las claves RSA", e);
        }
    }

    public RSAKey jwk() {
        return jwk;
    }

    public RSAPublicKey publica() {
        try {
            return jwk.toRSAPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
