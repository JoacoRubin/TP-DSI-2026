package ar.utn.donatrack.soporte.seguridad;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * El token de servicio se pide dentro de un método {@code synchronized} y desde el interceptor de TODAS las
 * llamadas servicio→servicio: si Autenticación se cuelga y no hay timeout, se traba cada hilo que llama a otro
 * servicio. Tiene que fallar rápido para que el error se propague (y el outbox/reintento haga su trabajo).
 */
class TokenServicioProviderTimeoutTest {

    private HttpServer servidor;

    private String autenticacionQueNoResponde() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            try { Thread.sleep(30_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            intercambio.sendResponseHeaders(200, -1);
            intercambio.close();
        });
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void apagar() {
        if (servidor != null) servidor.stop(0);
    }

    @Test
    void siAutenticacionNoRespondeFallaRapidoEnVezDeColgarse() throws Exception {
        PropiedadesClienteServicio propiedades =
                new PropiedadesClienteServicio(autenticacionQueNoResponde(), "logistica", "secreto");
        TokenServicioProvider provider = new SeguridadAutoConfiguration().tokenServicioProvider(propiedades);

        assertTimeoutPreemptively(Duration.ofSeconds(15), () ->
                assertThatThrownBy(provider::token).isNotInstanceOf(IllegalStateException.class));
    }
}
