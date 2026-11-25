package ar.utn.donatrack.planificador;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * El callback corre en el pool de @Async: si Logística no contesta y no hay timeout, cada planificación
 * deja un hilo colgado para siempre y el pool se agota (el proveedor deja de procesar solicitudes nuevas).
 */
class ProcesadorAsincronicoTimeoutTest {

    private HttpServer servidor;

    private String callbackQueNoResponde() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            try { Thread.sleep(30_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            intercambio.sendResponseHeaders(200, -1);
            intercambio.close();
        });
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/callback";
    }

    @AfterEach
    void apagar() {
        if (servidor != null) servidor.stop(0);
    }

    @Test
    void unCallbackQueNoRespondeNoDejaElHiloColgado() throws Exception {
        ProcesadorAsincronico procesador = new ProcesadorAsincronico(0, RestClient.builder());
        PlanificacionesController.Solicitud solicitud = new PlanificacionesController.Solicitud(
                UUID.randomUUID(),
                new Algoritmo.Punto(-34.60, -58.38),
                List.of(),
                List.of(),
                callbackQueNoResponde(),
                "token");

        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> procesador.procesar(solicitud));
    }
}
