package ar.utn.donatrack.autenticacion.configuracion;

import ar.utn.donatrack.autenticacion.usuarios.application.ActivarCuenta;
import ar.utn.donatrack.autenticacion.usuarios.application.EmisorTokens;
import ar.utn.donatrack.autenticacion.usuarios.application.EmitirTokenServicio;
import ar.utn.donatrack.autenticacion.usuarios.application.GeneradorSecretos;
import ar.utn.donatrack.autenticacion.usuarios.application.HasheadorClaves;
import ar.utn.donatrack.autenticacion.usuarios.application.IniciarSesion;
import ar.utn.donatrack.autenticacion.usuarios.application.RegistrarUsuario;
import ar.utn.donatrack.autenticacion.usuarios.application.RegistrarUsuariosEnLote;
import ar.utn.donatrack.autenticacion.usuarios.domain.UsuarioRepositorio;
import ar.utn.donatrack.autenticacion.usuarios.infrastructure.seguridad.ClavesRsa;
import ar.utn.donatrack.autenticacion.usuarios.infrastructure.seguridad.JwtEmisorTokens;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Composition root: acá (y sólo acá) se cablean casos de uso con sus adaptadores.
 * Los casos de uso son POJOs: no tienen anotaciones de Spring.
 */
@Configuration
public class ConfiguracionAutenticacion {

    @Bean
    ClavesRsa clavesRsa(PropiedadesAutenticacion props) {
        return ClavesRsa.cargarOGenerar(Path.of(props.directorioClaves()));
    }

    @Bean
    JwtEncoder jwtEncoder(ClavesRsa claves) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(claves.jwk())));
    }

    @Bean
    JwtDecoder jwtDecoder(ClavesRsa claves) {
        return NimbusJwtDecoder.withPublicKey(claves.publica()).build();
    }

    @Bean
    EmisorTokens emisorTokens(JwtEncoder encoder, ClavesRsa claves, Clock reloj, PropiedadesAutenticacion props) {
        return new JwtEmisorTokens(encoder, claves.jwk().getKeyID(), reloj, props.vigenciaTokenUsuario(),
                props.vigenciaTokenServicio());
    }

    @Bean
    IniciarSesion iniciarSesion(UsuarioRepositorio r, HasheadorClaves h, EmisorTokens e) {
        return new IniciarSesion(r, h, e);
    }

    @Bean
    RegistrarUsuario registrarUsuario(UsuarioRepositorio r, HasheadorClaves h, GeneradorSecretos s, Clock reloj) {
        return new RegistrarUsuario(r, h, s, reloj);
    }

    @Bean
    RegistrarUsuariosEnLote registrarUsuariosEnLote(UsuarioRepositorio r, GeneradorSecretos s, Clock reloj) {
        return new RegistrarUsuariosEnLote(r, s, reloj);
    }

    @Bean
    ActivarCuenta activarCuenta(UsuarioRepositorio r, HasheadorClaves h, Clock reloj) {
        return new ActivarCuenta(r, h, reloj);
    }

    @Bean
    EmitirTokenServicio emitirTokenServicio(PropiedadesAutenticacion props, EmisorTokens e) {
        return new EmitirTokenServicio(props.clientesServicio(), e);
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, JwtAuthenticationConverter conversor) throws Exception {
        return http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/**", "/.well-known/**", "/actuator/health/**", "/actuator/prometheus",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/**").hasAnyRole("SERVICIO", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)))
                .build();
    }
}
