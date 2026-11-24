package ar.utn.donatrack.observabilidad;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Las consultas de Prometheus y de los health checks (scripts, vigilante) a {@code /actuator/**}
 * no se observan: con muestreo al 100 % cada scrape era una traza y tapaba las del negocio en Tempo.
 * Tampoco cuentan en {@code http_server_requests}, que así mide sólo el tráfico real de usuarios.
 * Que el servicio esté vivo lo sigue midiendo {@code up} en Prometheus.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ServerRequestObservationContext.class)
public class ObservacionesWeb {

    @Bean
    ObservationPredicate sinActuator() {
        return (nombre, contexto) -> !(contexto instanceof ServerRequestObservationContext pedido
                && pedido.getCarrier().getRequestURI().startsWith("/actuator"));
    }
}
