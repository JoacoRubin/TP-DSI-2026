package ar.utn.donatrack.front.limites;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ConfiguracionLimites {

    @Bean
    FilterRegistrationBean<FiltroLimiteSolicitudes> filtroLimiteSolicitudes(MeterRegistry metricas) {
        FilterRegistrationBean<FiltroLimiteSolicitudes> registro =
                new FilterRegistrationBean<>(new FiltroLimiteSolicitudes(new LimitadorSolicitudes(), metricas));
        // Antes de la cadena de Spring Security (-100): el abuso se corta antes de autenticar.
        // Después del filtro de observación de Spring, así los 429 también quedan medidos.
        registro.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 10);
        return registro;
    }
}
