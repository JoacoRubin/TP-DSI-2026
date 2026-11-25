package ar.utn.donatrack.notificaciones.envios.infrastructure.canales;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Twilio es un servicio externo por Internet: si se pone lento, el envío tiene que fallar rápido (y quedar
 * registrado como fallido) en vez de trabar al consumidor de la cola de notificaciones.
 */
class CanalTwilioTimeoutTest {

    private HttpServer servidor;

    private String twilioQueNoResponde() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            try { Thread.sleep(30_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            intercambio.sendResponseHeaders(201, -1);
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
    void siTwilioNoRespondeElEnvioFallaRapido() throws Exception {
        CanalTwilio canal = new CanalTwilio(MedioNotificacion.SMS, "AC123", "token", "+5491100000000",
                twilioQueNoResponde());
        Destinatario ana = new Destinatario("Ana", "ana@donatrack.org", "+54 9 11 5555-0000", null, MedioNotificacion.SMS);

        assertTimeoutPreemptively(Duration.ofSeconds(20), () ->
                assertThatThrownBy(() -> canal.enviar(ana, "Aviso", "Tu donación llegó")).isNotNull());
    }
}
