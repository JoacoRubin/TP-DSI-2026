package ar.utn.donatrack.autenticacion.usuarios.application;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Flujo "client credentials": un servicio de DonaTrack se identifica para hablar con otro. */
public class EmitirTokenServicio {

    private final Map<String, String> clientesRegistrados;
    private final EmisorTokens emisor;

    public EmitirTokenServicio(Map<String, String> clientesRegistrados, EmisorTokens emisor) {
        this.clientesRegistrados = Map.copyOf(clientesRegistrados);
        this.emisor = emisor;
    }

    public TokenEmitido ejecutar(String clientId, String clientSecret) {
        String esperado = clientId == null ? null : clientesRegistrados.get(clientId);
        if (esperado == null || clientSecret == null
                || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), clientSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new CredencialesInvalidasException();
        }
        return emisor.paraServicio(clientId);
    }
}
