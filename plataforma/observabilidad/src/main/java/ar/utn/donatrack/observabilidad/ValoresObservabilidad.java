package ar.utn.donatrack.observabilidad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Valores por defecto de observabilidad para TODOS los procesos de DonaTrack, con la menor
 * precedencia: el application.yml de cada servicio (o una variable de entorno) los pisa.
 * <ul>
 *   <li><b>Métricas</b>: {@code /actuator/prometheus}, etiquetadas con el nombre del servicio.</li>
 *   <li><b>Trazas</b>: W3C {@code traceparent} entre servicios; exportación OTLP a Tempo sólo si
 *       {@code DONATRACK_TRAZAS=true} (así no se llenan los logs de errores si el stack no corre).</li>
 *   <li><b>Logs</b>: JSON ECS en {@code logs/<servicio>.json} (con traceId/spanId) para Loki;
 *       la consola sigue en texto legible.</li>
 * </ul>
 * Se ejecuta antes de que arranque el sistema de logging, por eso puede configurarlo.
 */
public class ValoresObservabilidad implements EnvironmentPostProcessor {

    static final String NOMBRE = "donatrackObservabilidad";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        env.getPropertySources().addLast(new MapPropertySource(NOMBRE, Map.ofEntries(
                Map.entry("management.endpoints.web.exposure.include", "health,info,prometheus"),
                Map.entry("management.endpoint.health.probes.enabled", "true"),
                Map.entry("management.metrics.tags.application", "${spring.application.name:donatrack}"),
                Map.entry("management.tracing.sampling.probability", "1.0"),
                // Spans internos de Spring Security (filterchain, authorize): ruido en cada request.
                Map.entry("management.observations.enable.spring.security", "false"),
                // @Scheduled (p. ej. outbox cada 3 s): una traza vacía por ejecución.
                Map.entry("management.observations.enable.tasks.scheduled", "false"),
                Map.entry("management.otlp.tracing.export.enabled", "${DONATRACK_TRAZAS:false}"),
                Map.entry("management.otlp.tracing.endpoint", "${OTLP_URL:http://localhost:4318}/v1/traces"),
                Map.entry("logging.structured.format.file", "ecs"),
                Map.entry("logging.structured.ecs.service.name", "${spring.application.name:donatrack}"),
                Map.entry("logging.file.name", "${DONATRACK_LOGS:logs}/${spring.application.name:donatrack}.json"))));
    }
}
