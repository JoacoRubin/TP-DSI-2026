package ar.utn.donatrack.soporte.seguridad;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;

/**
 * Configuración común de los servicios como "resource servers": cada JWT emitido por el
 * Servicio de Autenticación trae un claim {@code rol} que se traduce a {@code ROLE_<rol>}.
 */
@AutoConfiguration
public class SeguridadAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString(Claims.ROL);
            Collection<GrantedAuthority> autoridades = rol == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            return autoridades;
        });
        return conversor;
    }

    @Bean
    @ConditionalOnProperty(prefix = "donatrack.seguridad.cliente", name = "id")
    public TokenServicioProvider tokenServicioProvider(PropiedadesClienteServicio propiedades) {
        return new TokenServicioProvider(RestClient.builder().baseUrl(propiedades.authUrl()).build(), propiedades);
    }

    @Bean
    @ConditionalOnProperty(prefix = "donatrack.seguridad.cliente", name = "id")
    public PropiedadesClienteServicio propiedadesClienteServicio(
            org.springframework.core.env.Environment env) {
        return new PropiedadesClienteServicio(
                env.getRequiredProperty("donatrack.seguridad.cliente.auth-url"),
                env.getRequiredProperty("donatrack.seguridad.cliente.id"),
                env.getRequiredProperty("donatrack.seguridad.cliente.secreto"));
    }
}
