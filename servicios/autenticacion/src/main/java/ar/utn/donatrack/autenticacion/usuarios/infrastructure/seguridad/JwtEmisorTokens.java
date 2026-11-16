package ar.utn.donatrack.autenticacion.usuarios.infrastructure.seguridad;

import ar.utn.donatrack.autenticacion.usuarios.application.EmisorTokens;
import ar.utn.donatrack.autenticacion.usuarios.application.TokenEmitido;
import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;
import ar.utn.donatrack.soporte.seguridad.Claims;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class JwtEmisorTokens implements EmisorTokens {

    public static final String EMISOR = "donatrack-autenticacion";

    private final JwtEncoder encoder;
    private final String kid;
    private final Clock reloj;
    private final Duration vigenciaUsuario;
    private final Duration vigenciaServicio;

    public JwtEmisorTokens(JwtEncoder encoder, String kid, Clock reloj, Duration vigenciaUsuario, Duration vigenciaServicio) {
        this.encoder = encoder;
        this.kid = kid;
        this.reloj = reloj;
        this.vigenciaUsuario = vigenciaUsuario;
        this.vigenciaServicio = vigenciaServicio;
    }

    @Override
    public TokenEmitido paraUsuario(Usuario u) {
        JwtClaimsSet.Builder claims = base(u.id().toString(), vigenciaUsuario)
                .claim(Claims.ROL, u.rol().name())
                .claim(Claims.EMAIL, u.email())
                .claim(Claims.NOMBRE, u.nombre());
        if (u.referencia() != null) {
            claims.claim(Claims.REFERENCIA, u.referencia());
        }
        if (u.perfil() != null) {
            claims.claim(Claims.PERFIL, u.perfil());
        }
        return firmar(claims.build(), vigenciaUsuario);
    }

    @Override
    public TokenEmitido paraServicio(String clientId) {
        return firmar(base("servicio:" + clientId, vigenciaServicio)
                .claim(Claims.ROL, "SERVICIO")
                .claim(Claims.EMAIL, clientId + "@servicios.donatrack")
                .build(), vigenciaServicio);
    }

    private JwtClaimsSet.Builder base(String sujeto, Duration vigencia) {
        Instant ahora = reloj.instant();
        return JwtClaimsSet.builder().issuer(EMISOR).subject(sujeto).issuedAt(ahora).expiresAt(ahora.plus(vigencia));
    }

    private TokenEmitido firmar(JwtClaimsSet claims, Duration vigencia) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(kid).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, vigencia.toSeconds());
    }
}
