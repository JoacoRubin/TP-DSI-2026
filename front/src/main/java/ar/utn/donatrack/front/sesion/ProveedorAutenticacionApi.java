package ar.utn.donatrack.front.sesion;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ErrorApi;
import ar.utn.donatrack.front.api.ServicioNoDisponible;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Delega la verificación de credenciales al Servicio de Autenticación. */
@Component
class ProveedorAutenticacionApi implements AuthenticationProvider {

    private final Apis apis;

    ProveedorAutenticacionApi(Apis apis) {
        this.apis = apis;
    }

    @Override
    public Authentication authenticate(Authentication auth) {
        String email = auth.getName();
        String clave = auth.getCredentials() == null ? "" : auth.getCredentials().toString();
        Map<String, Object> sesion;
        try {
            sesion = apis.autenticacion().post("/api/auth/login", Map.of("email", email, "clave", clave));
        } catch (ErrorApi e) {
            if (e.estado() == 422) {
                throw new DisabledException(e.getMessage());
            }
            throw new BadCredentialsException(e.getMessage());
        } catch (ServicioNoDisponible e) {
            throw new AuthenticationServiceException(e.getMessage(), e);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> token = (Map<String, Object>) sesion.get("token");
        UsuarioSesion u = new UsuarioSesion(str(sesion.get("usuarioId")), str(sesion.get("email")), str(sesion.get("nombre")),
                str(sesion.get("rol")), str(sesion.get("perfil")), str(sesion.get("referencia")), str(token.get("accessToken")));
        return new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.rol())));
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
