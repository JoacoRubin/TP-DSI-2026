package ar.utn.donatrack.observabilidad;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValoresObservabilidadTest {

    private StandardEnvironment entorno(Map<String, Object> propiasDelServicio) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("application.yml", propiasDelServicio));
        new ValoresObservabilidad().postProcessEnvironment(env, new SpringApplication());
        return env;
    }

    @Test
    void exponeMetricasTrazasYLogsPorDefecto() {
        StandardEnvironment env = entorno(Map.of("spring.application.name", "donaciones"));

        assertThat(env.getProperty("management.endpoints.web.exposure.include")).contains("prometheus");
        assertThat(env.getProperty("management.metrics.tags.application")).isEqualTo("donaciones");
        assertThat(env.getProperty("management.tracing.sampling.probability")).isEqualTo("1.0");
        assertThat(env.getProperty("logging.structured.format.file")).isEqualTo("ecs");
        assertThat(env.getProperty("logging.file.name")).isEqualTo("logs/donaciones.json");
    }

    @Test
    void elEnvioDeTrazasArrancaApagadoYSeEnciendeConVariable() {
        assertThat(entorno(Map.of()).getProperty("management.otlp.tracing.export.enabled")).isEqualTo("false");

        StandardEnvironment env = entorno(Map.of("DONATRACK_TRAZAS", "true", "OTLP_URL", "http://tempo:4318"));
        assertThat(env.getProperty("management.otlp.tracing.export.enabled")).isEqualTo("true");
        assertThat(env.getProperty("management.otlp.tracing.endpoint")).isEqualTo("http://tempo:4318/v1/traces");
    }

    @Test
    void losSpansInternosDeSpringSecurityNoSeTrazan() {
        // Cada request generaba 4-5 spans (filterchain before/after, authorize...) que tapan los útiles.
        assertThat(entorno(Map.of()).getProperty("management.observations.enable.spring.security")).isEqualTo("false");
    }

    @Test
    void lasTareasProgramadasNoGeneranUnaTrazaPorEjecucion() {
        // El despacho del outbox corre cada 3 s: 18 trazas vacías en 4 minutos. Si despacha algo,
        // la llamada HTTP a Logística igual se traza (como raíz propia).
        assertThat(entorno(Map.of()).getProperty("management.observations.enable.tasks.scheduled")).isEqualTo("false");
    }

    @Test
    void laConfiguracionDelServicioTienePrioridad() {
        StandardEnvironment env = entorno(Map.of("management.tracing.sampling.probability", "0.1"));
        assertThat(env.getProperty("management.tracing.sampling.probability")).isEqualTo("0.1");
    }
}
