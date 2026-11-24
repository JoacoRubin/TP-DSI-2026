package ar.utn.donatrack.observabilidad;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validado con el stack levantado: Prometheus consulta /actuator/prometheus de 7 procesos cada 15 s y
 * con muestreo al 100 % cada consulta era una traza; las del negocio quedaban enterradas en Tempo.
 */
class SinObservarActuatorTest {

    private final ObservationPredicate predicado = new ObservacionesWeb().sinActuator();

    private static ServerRequestObservationContext pedido(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        return new ServerRequestObservationContext(r, new MockHttpServletResponse());
    }

    @Test
    void lasConsultasAlActuatorNoSeObservan() {
        assertThat(predicado.test("http.server.requests", pedido("/actuator/prometheus"))).isFalse();
        assertThat(predicado.test("http.server.requests", pedido("/actuator/health"))).isFalse();
    }

    @Test
    void elRestoDeLosPedidosSiSeObserva() {
        assertThat(predicado.test("http.server.requests", pedido("/api/publico/resumen"))).isTrue();
        assertThat(predicado.test("http.server.requests", pedido("/"))).isTrue();
        assertThat(predicado.test("otra", new Observation.Context())).isTrue();
    }

    @Test
    void seRegistraSolaEnAplicacionesWeb() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ObservacionesWeb.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(ObservationPredicate.class));
    }
}
