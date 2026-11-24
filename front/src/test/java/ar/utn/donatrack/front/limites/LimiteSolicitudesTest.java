package ar.utn.donatrack.front.limites;

import io.github.bucket4j.TimeMeter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteSolicitudesTest {

    /** Reloj manual: permite probar la recarga del bucket sin dormir el test. */
    static class RelojManual implements TimeMeter {
        long nanos;
        public long currentTimeNanos() { return nanos; }
        public boolean isWallClockBased() { return false; }
        void avanzar(Duration d) { nanos += d.toNanos(); }
    }

    private final RelojManual reloj = new RelojManual();
    private final SimpleMeterRegistry metricas = new SimpleMeterRegistry();
    private final FiltroLimiteSolicitudes filtro = new FiltroLimiteSolicitudes(new LimitadorSolicitudes(reloj), metricas);

    private MockHttpServletResponse enviar(String metodo, String uri, String ip) throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest(metodo, uri);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void clasificaCadaSolicitudEnSuPolitica() {
        assertThat(PoliticaLimite.para("POST", "/login")).contains(PoliticaLimite.AUTENTICACION);
        assertThat(PoliticaLimite.para("POST", "/registro/humana")).contains(PoliticaLimite.AUTENTICACION);
        assertThat(PoliticaLimite.para("POST", "/front-api/chofer/rutas/123/posiciones")).contains(PoliticaLimite.GPS);
        assertThat(PoliticaLimite.para("GET", "/mapa")).contains(PoliticaLimite.GENERAL);
        assertThat(PoliticaLimite.para("GET", "/js/donatrack.js")).isEmpty();
        assertThat(PoliticaLimite.para("GET", "/actuator/health")).isEmpty();
    }

    @Test
    void frenaLaFuerzaBrutaDelLoginCon429YRetryAfter() throws Exception {
        for (int i = 0; i < PoliticaLimite.AUTENTICACION.capacidad(); i++) {
            assertThat(enviar("POST", "/login", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse rechazada = enviar("POST", "/login", "10.0.0.1");

        assertThat(rechazada.getStatus()).isEqualTo(429);
        assertThat(Integer.parseInt(rechazada.getHeader("Retry-After"))).isPositive();
        assertThat(metricas.counter("donatrack.limite.rechazos", "politica", "AUTENTICACION").count()).isEqualTo(1);
    }

    @Test
    void cadaClienteTieneSuPropioCupo() throws Exception {
        for (int i = 0; i < PoliticaLimite.AUTENTICACION.capacidad(); i++) {
            enviar("POST", "/login", "10.0.0.1");
        }
        assertThat(enviar("POST", "/login", "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(enviar("POST", "/login", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void elCupoSeRecargaConElTiempo() throws Exception {
        for (int i = 0; i <= PoliticaLimite.AUTENTICACION.capacidad(); i++) {
            enviar("POST", "/login", "10.0.0.1");
        }
        reloj.avanzar(Duration.ofMinutes(1));
        assertThat(enviar("POST", "/login", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void losRecursosEstaticosNoConsumenCupo() throws Exception {
        for (int i = 0; i < PoliticaLimite.GENERAL.capacidad() + 10; i++) {
            assertThat(enviar("GET", "/css/estilos.css", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }
}
