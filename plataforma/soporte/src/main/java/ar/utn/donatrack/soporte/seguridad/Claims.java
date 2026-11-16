package ar.utn.donatrack.soporte.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

/** Nombres de claims del JWT de DonaTrack y helpers de lectura. */
public final class Claims {
    public static final String ROL = "rol";
    public static final String EMAIL = "email";
    public static final String NOMBRE = "nombre";
    /** Id del donante / entidad en el Servicio de Donaciones, o patente del camión para choferes. */
    public static final String REFERENCIA = "ref";
    /** HUMANA / JURIDICA para donantes. */
    public static final String PERFIL = "perfil";

    private Claims() {
    }

    public static Optional<Jwt> jwt(Authentication auth) {
        return auth instanceof JwtAuthenticationToken t ? Optional.of(t.getToken()) : Optional.empty();
    }

    public static Optional<String> referencia(Authentication auth) {
        return jwt(auth).map(j -> j.getClaimAsString(REFERENCIA));
    }

    public static Optional<UUID> referenciaUuid(Authentication auth) {
        return referencia(auth).filter(r -> !r.isBlank()).map(UUID::fromString);
    }

    public static boolean tieneRol(Authentication auth, String rol) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }

    public static String usuario(Authentication auth) {
        return auth == null ? "sistema" : jwt(auth).map(j -> j.getClaimAsString(EMAIL)).orElse(auth.getName());
    }
}
