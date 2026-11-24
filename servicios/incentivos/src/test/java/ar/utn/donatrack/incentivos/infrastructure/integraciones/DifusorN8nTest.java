package ar.utn.donatrack.incentivos.infrastructure.integraciones;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.incentivos.misiones.domain.CategoriaDonante;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Hallazgo de la demo en vivo: el HttpClient del JDK pide "Upgrade: h2c"; n8n se queda con esa conexión y
 * nunca responde, y como la llamada no tenía timeout, el ÚNICO consumidor de la cola de Incentivos quedó
 * bloqueado (ninguna donación nueva sumaba misiones). La difusión es best effort: jamás puede trabar la cola.
 */
class DifusorN8nTest {

    private HttpServer servidor;

    private String levantar(boolean colgarSiempre, AtomicReference<String> upgradeRecibido) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/webhook", intercambio -> {
            String upgrade = intercambio.getRequestHeaders().getFirst("Upgrade");
            upgradeRecibido.set(upgrade == null ? "" : upgrade);
            if (colgarSiempre || upgrade != null) { // como n8n con "Upgrade: h2c": no contesta nunca
                try { Thread.sleep(30_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            intercambio.sendResponseHeaders(200, -1);
            intercambio.close();
        });
        servidor.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/webhook";
    }

    @AfterEach
    void apagar() {
        if (servidor != null) servidor.stop(0);
    }

    private static PerfilDonante lucia() {
        return PerfilDonante.nuevo(UUID.randomUUID(), "Lucía Gómez", "lucia@donatrack.org", null, null,
                MedioNotificacion.EMAIL, true, Instant.now());
    }

    private static InsigniaObtenida primerPaso(PerfilDonante p) {
        return InsigniaObtenida.nueva(p.donanteId(), "primer-paso", "Primer paso", "🌱", CategoriaDonante.COLABORADOR, Instant.now());
    }

    @Test
    void hablaHttp11SinPedirUpgradeYElWebhookResponde() throws Exception {
        AtomicReference<String> upgrade = new AtomicReference<>();
        DifusorN8n difusor = new DifusorN8n(levantar(false, upgrade), RestClient.builder());
        PerfilDonante p = lucia();

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> difusor.difundir(p, primerPaso(p), "texto"));
        assertThat(upgrade.get()).isEmpty();
    }

    @Test
    void unWebhookQueNoRespondeNoTrabaAlConsumidor() throws Exception {
        DifusorN8n difusor = new DifusorN8n(levantar(true, new AtomicReference<>()), RestClient.builder());
        PerfilDonante p = lucia();

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThatCode(() -> difusor.difundir(p, primerPaso(p), "texto")).doesNotThrowAnyException());
    }
}
