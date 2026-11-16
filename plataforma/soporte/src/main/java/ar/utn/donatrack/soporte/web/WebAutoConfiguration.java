package ar.utn.donatrack.soporte.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.ZoneId;

@AutoConfiguration
@Import(ManejadorErroresApi.class)
public class WebAutoConfiguration {

    /** Reloj inyectable: los casos de uso nunca llaman a now() directo, así se testean. */
    @Bean
    @ConditionalOnMissingBean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openApi(@Value("${spring.application.name:donatrack}") String nombre) {
        return new OpenAPI()
                .info(new Info().title("DonaTrack - " + nombre).version("1.0"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
