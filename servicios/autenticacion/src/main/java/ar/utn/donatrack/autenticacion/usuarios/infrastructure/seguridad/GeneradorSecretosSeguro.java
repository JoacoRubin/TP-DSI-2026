package ar.utn.donatrack.autenticacion.usuarios.infrastructure.seguridad;

import ar.utn.donatrack.autenticacion.usuarios.application.GeneradorSecretos;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** 256 bits de entropía, codificados en Base64 URL-safe (aptos para ir en un enlace). */
@Component
class GeneradorSecretosSeguro implements GeneradorSecretos {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nuevo() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
