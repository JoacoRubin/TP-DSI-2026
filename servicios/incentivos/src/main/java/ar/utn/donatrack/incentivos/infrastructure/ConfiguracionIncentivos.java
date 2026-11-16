package ar.utn.donatrack.incentivos.infrastructure;

import ar.utn.donatrack.incentivos.actividad.domain.ActividadRepositorio;
import ar.utn.donatrack.incentivos.misiones.application.ActualizarPreferencias;
import ar.utn.donatrack.incentivos.misiones.application.AplicadorProgreso;
import ar.utn.donatrack.incentivos.misiones.application.ConsultasIncentivos;
import ar.utn.donatrack.incentivos.misiones.application.ProcesarDonacionEntregada;
import ar.utn.donatrack.incentivos.misiones.application.ProcesarDonacionRegistrada;
import ar.utn.donatrack.incentivos.misiones.application.Puertos;
import ar.utn.donatrack.incentivos.misiones.domain.CatalogoMisiones;
import ar.utn.donatrack.incentivos.misiones.domain.MotorIncentivos;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;
import ar.utn.donatrack.incentivos.ranking.application.Rankings;
import ar.utn.donatrack.incentivos.ranking.domain.RankingRepositorio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;

@Configuration
public class ConfiguracionIncentivos {

    @Bean
    MotorIncentivos motorIncentivos() {
        return new MotorIncentivos(CatalogoMisiones.predeterminado());
    }

    @Bean
    AplicadorProgreso aplicadorProgreso(MotorIncentivos m, ActividadRepositorio a, Repositorios.PerfilRepositorio p,
                                        Repositorios.InsigniaRepositorio i, Puertos.Notificador n,
                                        Puertos.DifusorLogros d, Clock reloj) {
        return new AplicadorProgreso(m, a, p, i, n, d, reloj);
    }

    @Bean
    ProcesarDonacionRegistrada procesarDonacionRegistrada(ActividadRepositorio a, Repositorios.PerfilRepositorio p,
                                                          AplicadorProgreso ap, Clock reloj) {
        return new ProcesarDonacionRegistrada(a, p, ap, reloj);
    }

    @Bean
    ProcesarDonacionEntregada procesarDonacionEntregada(ActividadRepositorio a, Repositorios.PerfilRepositorio p,
                                                        AplicadorProgreso ap) {
        return new ProcesarDonacionEntregada(a, p, ap);
    }

    @Bean
    Rankings rankings(RankingRepositorio r, Repositorios.InsigniaRepositorio i, Repositorios.PerfilRepositorio p, Clock reloj) {
        return new Rankings(r, i, p, reloj);
    }

    @Bean
    ConsultasIncentivos consultasIncentivos(Repositorios.PerfilRepositorio p, Repositorios.InsigniaRepositorio i,
                                            ActividadRepositorio a, MotorIncentivos m, Rankings r, Clock reloj) {
        return new ConsultasIncentivos(p, i, a, m, r, reloj);
    }

    @Bean
    ActualizarPreferencias actualizarPreferencias(Repositorios.PerfilRepositorio p) {
        return new ActualizarPreferencias(p);
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, JwtAuthenticationConverter conversor) throws Exception {
        return http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/publico/**", "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/rankings/**", "/api/rankings").hasRole("ADMIN")
                        .requestMatchers("/api/donantes/**").hasAnyRole("DONANTE", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)))
                .build();
    }
}
