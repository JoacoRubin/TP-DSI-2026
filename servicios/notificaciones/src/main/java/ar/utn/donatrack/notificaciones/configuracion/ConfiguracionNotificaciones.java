package ar.utn.donatrack.notificaciones.configuracion;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.notificaciones.envios.application.ConsultarNotificaciones;
import ar.utn.donatrack.notificaciones.envios.application.EnviarNotificacion;
import ar.utn.donatrack.notificaciones.envios.application.ReintentarFallidas;
import ar.utn.donatrack.notificaciones.envios.domain.CanalNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.NotificacionRepositorio;
import ar.utn.donatrack.notificaciones.envios.domain.SelectorCanal;
import ar.utn.donatrack.notificaciones.envios.infrastructure.canales.CanalEmailSmtp;
import ar.utn.donatrack.notificaciones.envios.infrastructure.canales.CanalSimulado;
import ar.utn.donatrack.notificaciones.envios.infrastructure.canales.CanalTwilio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;
import java.util.List;

@Configuration
public class ConfiguracionNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionNotificaciones.class);

    /** Elige los adaptadores según configuración: simulados en local, reales con credenciales. */
    @Bean
    SelectorCanal selectorCanal(PropiedadesNotificaciones props, ObjectProvider<JavaMailSender> mailSender) {
        CanalNotificacion email = "smtp".equalsIgnoreCase(props.emailModo())
                ? new CanalEmailSmtp(mailSender.getObject(), props.remitente())
                : new CanalSimulado(MedioNotificacion.EMAIL);
        boolean twilioReal = "real".equalsIgnoreCase(props.twilioModo());
        CanalNotificacion sms = twilioReal
                ? new CanalTwilio(MedioNotificacion.SMS, props.twilioAccountSid(), props.twilioAuthToken(), props.twilioNumeroSms())
                : new CanalSimulado(MedioNotificacion.SMS);
        CanalNotificacion whatsapp = twilioReal
                ? new CanalTwilio(MedioNotificacion.WHATSAPP, props.twilioAccountSid(), props.twilioAuthToken(), props.twilioNumeroWhatsapp())
                : new CanalSimulado(MedioNotificacion.WHATSAPP);
        log.info("Canales de notificación → EMAIL: {}, SMS/WHATSAPP: {}",
                email.getClass().getSimpleName(), sms.getClass().getSimpleName());
        return new SelectorCanal(List.of(email, sms, whatsapp));
    }

    @Bean
    EnviarNotificacion enviarNotificacion(NotificacionRepositorio r, SelectorCanal s, Clock reloj) {
        return new EnviarNotificacion(r, s, reloj);
    }

    @Bean
    ReintentarFallidas reintentarFallidas(NotificacionRepositorio r, SelectorCanal s, Clock reloj) {
        return new ReintentarFallidas(r, s, reloj);
    }

    @Bean
    ConsultarNotificaciones consultarNotificaciones(NotificacionRepositorio r) {
        return new ConsultarNotificaciones(r);
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, JwtAuthenticationConverter conversor) throws Exception {
        return http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/notificaciones").hasAnyRole("ADMIN", "SERVICIO")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)))
                .build();
    }
}
