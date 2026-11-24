package ar.utn.donatrack.logistica.configuracion;

import ar.utn.donatrack.logistica.compartido.application.PublicadorEventos;
import ar.utn.donatrack.logistica.compartido.infrastructure.integraciones.PlanificadorRutasRest;
import ar.utn.donatrack.logistica.entregas.application.AgregarFotos;
import ar.utn.donatrack.logistica.entregas.application.CancelarEntrega;
import ar.utn.donatrack.logistica.entregas.application.CierreDeRuta;
import ar.utn.donatrack.logistica.entregas.application.ConfirmarRecepcion;
import ar.utn.donatrack.logistica.entregas.application.CrearEntrega;
import ar.utn.donatrack.logistica.entregas.application.InformarNoRecepcion;
import ar.utn.donatrack.logistica.entregas.application.ReprogramarEntrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.flota.application.GestionarFlota;
import ar.utn.donatrack.logistica.flota.domain.CamionRepositorio;
import ar.utn.donatrack.logistica.rutas.application.ConsultasLogistica;
import ar.utn.donatrack.logistica.rutas.application.IniciarRuta;
import ar.utn.donatrack.logistica.rutas.application.PlanificadorRutas;
import ar.utn.donatrack.logistica.rutas.application.PlanificarRutas;
import ar.utn.donatrack.logistica.rutas.application.RegistrarPosicion;
import ar.utn.donatrack.logistica.rutas.application.RegistrarResultadoPlanificacion;
import ar.utn.donatrack.logistica.rutas.domain.RutaRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacionRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;

/** Composition root del Servicio de Logística. */
@Configuration
public class ConfiguracionLogistica {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionLogistica.class);

    @Bean
    PlanificadorRutas planificadorRutas(@Value("${donatrack.logistica.url-planificador}") String url,
                                        @Value("${donatrack.logistica.url-callback}") String callback,
                                        @Value("${donatrack.logistica.token-callback}") String token,
                                        io.micrometer.observation.ObservationRegistry observaciones) {
        return new PlanificadorRutasRest(url, callback, token, observaciones);
    }

    @Bean
    PlanificadorRutas.Ubicacion deposito(@Value("${donatrack.logistica.deposito.latitud}") double lat,
                                         @Value("${donatrack.logistica.deposito.longitud}") double lng) {
        return new PlanificadorRutas.Ubicacion(lat, lng);
    }

    @Bean
    CrearEntrega crearEntrega(EntregaRepositorio e, Clock reloj) {
        return new CrearEntrega(e, reloj);
    }

    @Bean
    CancelarEntrega cancelarEntrega(EntregaRepositorio e, Clock reloj) {
        return new CancelarEntrega(e, reloj);
    }

    @Bean
    CierreDeRuta cierreDeRuta(EntregaRepositorio e, RutaRepositorio r) {
        return new CierreDeRuta(e, r);
    }

    @Bean
    ConfirmarRecepcion confirmarRecepcion(EntregaRepositorio e, CierreDeRuta c, PublicadorEventos ev, Clock reloj) {
        return new ConfirmarRecepcion(e, c, ev, reloj);
    }

    @Bean
    AgregarFotos agregarFotos(EntregaRepositorio e) {
        return new AgregarFotos(e);
    }

    @Bean
    InformarNoRecepcion informarNoRecepcion(EntregaRepositorio e, CierreDeRuta c, PublicadorEventos ev, Clock reloj) {
        return new InformarNoRecepcion(e, c, ev, reloj);
    }

    @Bean
    ReprogramarEntrega reprogramarEntrega(EntregaRepositorio e, PublicadorEventos ev, Clock reloj) {
        return new ReprogramarEntrega(e, ev, reloj);
    }

    @Bean
    PlanificarRutas planificarRutas(EntregaRepositorio e, CamionRepositorio c, SolicitudPlanificacionRepositorio s,
                                    PlanificadorRutas p, PlanificadorRutas.Ubicacion deposito, Clock reloj) {
        return new PlanificarRutas(e, c, s, p, deposito, reloj);
    }

    @Bean
    RegistrarResultadoPlanificacion registrarResultadoPlanificacion(SolicitudPlanificacionRepositorio s, EntregaRepositorio e,
                                                                    CamionRepositorio c, RutaRepositorio r,
                                                                    PublicadorEventos ev, Clock reloj) {
        return new RegistrarResultadoPlanificacion(s, e, c, r, ev, reloj);
    }

    @Bean
    IniciarRuta iniciarRuta(RutaRepositorio r, EntregaRepositorio e, PublicadorEventos ev,
                            @Value("${donatrack.logistica.url-seguimiento}") String url, Clock reloj) {
        return new IniciarRuta(r, e, ev, url, reloj);
    }

    @Bean
    RegistrarPosicion registrarPosicion(RutaRepositorio r, Clock reloj) {
        return new RegistrarPosicion(r, reloj);
    }

    @Bean
    GestionarFlota gestionarFlota(CamionRepositorio c) {
        return new GestionarFlota(c);
    }

    @Bean
    ConsultasLogistica consultasLogistica(RutaRepositorio r, EntregaRepositorio e, SolicitudPlanificacionRepositorio s,
                                          Clock reloj) {
        return new ConsultasLogistica(r, e, s, reloj);
    }

    @Bean
    TareaPlanificacionNocturna tareaPlanificacionNocturna(PlanificarRutas planificar) {
        return new TareaPlanificacionNocturna(planificar);
    }

    /** Genera en horario de baja carga los planes de ruta de la jornada siguiente. */
    static class TareaPlanificacionNocturna {
        private final PlanificarRutas planificar;

        TareaPlanificacionNocturna(PlanificarRutas planificar) {
            this.planificar = planificar;
        }

        @Scheduled(cron = "${donatrack.cron.planificacion:0 0 2 * * *}", zone = "America/Argentina/Buenos_Aires")
        void ejecutar() {
            try {
                log.info("Planificación nocturna: {} lotes enviados", planificar.ejecutar());
            } catch (RuntimeException e) {
                log.error("Falló la planificación nocturna: {}", e.getMessage());
            }
        }
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, JwtAuthenticationConverter conversor) throws Exception {
        return http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/publico/**", "/api/planificaciones/callback", "/actuator/health/**", "/actuator/prometheus",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/fotos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/entregas").hasAnyRole("SERVICIO", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/entregas/**").hasAnyRole("SERVICIO", "ADMIN")
                        .requestMatchers("/graphql").hasRole("ADMIN")
                        .requestMatchers("/api/entregas/*/reprogramacion", "/api/planificaciones/**", "/api/planificaciones",
                                "/api/camiones/**", "/api/camiones", "/api/monitoreo", "/api/rutas").hasRole("ADMIN")
                        .requestMatchers("/api/chofer/**", "/api/rutas/*/posiciones").hasAnyRole("CHOFER")
                        .requestMatchers("/api/rutas/*/inicio").hasAnyRole("CHOFER", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)))
                .build();
    }
}
