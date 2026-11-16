package ar.utn.donatrack.donaciones.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autorización en dos niveles: por ROL acá (grano grueso) y por RECURSO en los casos de uso
 * (ej. una entidad sólo edita SUS necesidades) mediante {@code Solicitante}.
 */
@Configuration
public class ConfiguracionSeguridad {

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, JwtAuthenticationConverter conversor) throws Exception {
        return http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/saludo", "/api/categorias", "/api/registro/**", "/actuator/health/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/fotos/**").permitAll()
                        .requestMatchers("/api/importaciones/**", "/api/matchmaking/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/donantes/*/interacciones").hasAnyRole("DONANTE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/donantes/*").hasAnyRole("DONANTE", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/donantes/*/contacto").hasAnyRole("DONANTE", "ADMIN")
                        .requestMatchers("/api/donantes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/donaciones", "/api/fotos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/donaciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/donaciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/donaciones/*/asignacion").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/entidades").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/entidades/*").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)))
                .build();
    }
}
