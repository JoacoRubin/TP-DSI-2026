package ar.utn.donatrack.front.sesion;

import ar.utn.donatrack.front.api.Apis;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Seguridad del front (BFF): sesión server-side con cookie HttpOnly, CSRF activo en todos los
 * formularios, autorización por rol en cada sección.
 */
@Configuration
public class ConfiguracionSeguridadFront {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSeguridadFront.class);

    @Bean
    SecurityFilterChain seguridadFront(HttpSecurity http, AuthenticationSuccessHandler exito) throws Exception {
        return http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/galeria", "/mapa", "/legal", "/accesibilidad", "/login", "/registro/**",
                                "/activar", "/fotos/**", "/front-api/publico/**", "/css/**", "/js/**", "/img/**",
                                "/favicon.svg", "/error", "/actuator/health").permitAll()
                        .requestMatchers("/donante/incentivos/**").access((auth, ctx) -> new org.springframework.security.authorization.AuthorizationDecision(
                                auth.get().getPrincipal() instanceof UsuarioSesion u && u.esDonanteHumano()))
                        .requestMatchers("/donante/**").hasRole("DONANTE")
                        .requestMatchers("/entidad/**").hasRole("BENEFICIARIA")
                        .requestMatchers("/admin/**", "/front-api/monitoreo", "/front-api/importaciones/**").hasRole("ADMIN")
                        .requestMatchers("/chofer/**", "/front-api/chofer/**").hasRole("CHOFER")
                        .anyRequest().authenticated())
                .formLogin(f -> f
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("clave")
                        .successHandler(exito)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(l -> l.logoutUrl("/salir").logoutSuccessUrl("/?salida").deleteCookies("JSESSIONID"))
                .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()).maximumSessions(3))
                .headers(h -> h
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(fo -> fo.deny()))
                .exceptionHandling(e -> e.accessDeniedPage("/error/403"))
                .build();
    }

    /** Tras el login: cada rol aterriza en su sección. Para donantes, se registra la interacción. */
    @Bean
    AuthenticationSuccessHandler exitoLogin(Apis apis) {
        return (request, response, authentication) -> {
            UsuarioSesion u = (UsuarioSesion) authentication.getPrincipal();
            String destino = switch (u.rol()) {
                case "ADMIN" -> "/admin";
                case "DONANTE" -> "/donante";
                case "BENEFICIARIA" -> "/entidad";
                case "CHOFER" -> "/chofer";
                default -> "/";
            };
            if ("DONANTE".equals(u.rol()) && u.referencia() != null) {
                try {
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
                    apis.donaciones().post("/api/donantes/{id}/interacciones", null, u.referencia());
                } catch (RuntimeException e) {
                    log.debug("No se pudo registrar la interacción: {}", e.getMessage());
                }
            }
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect(request.getContextPath() + destino);
        };
    }
}
