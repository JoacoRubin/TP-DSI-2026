package ar.utn.donatrack.planificador;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class PlanificacionesController {

    private final ProcesadorAsincronico procesador;

    PlanificacionesController(ProcesadorAsincronico procesador) {
        this.procesador = procesador;
    }

    record Solicitud(@NotNull UUID solicitudId, @NotNull Algoritmo.Punto deposito, @NotEmpty List<Algoritmo.Camion> camiones,
                     @NotEmpty @Size(max = 100, message = "El proveedor procesa como máximo 100 entregas por ejecución")
                     List<Algoritmo.Entrega> entregas,
                     @NotNull String callbackUrl, String callbackToken) {
    }

    @PostMapping("/api/planificaciones")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, Object> planificar(@Valid @RequestBody Solicitud s) {
        procesador.procesar(s);
        return Map.of("solicitudId", s.solicitudId(), "estado", "EN_PROCESO");
    }
}

@Component
class ProcesadorAsincronico {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorAsincronico.class);
    private final RestClient cliente;
    private final long demoraMs;

    ProcesadorAsincronico(@Value("${planificador.demora-ms:2000}") long demoraMs, RestClient.Builder clientes) {
        this.demoraMs = demoraMs;
        this.cliente = clientes.build(); // builder de Spring: ya instrumentado para trazas
    }

    record Callback(UUID solicitudId, List<Algoritmo.Ruta> rutas, List<UUID> sinAsignar) {
    }

    @Async
    public void procesar(PlanificacionesController.Solicitud s) {
        try {
            Thread.sleep(demoraMs); // simula el tiempo de cómputo del proveedor
            Algoritmo.Resultado r = Algoritmo.planificar(s.deposito(), s.camiones(), s.entregas());
            cliente.post().uri(s.callbackUrl())
                    .header("X-Callback-Token", s.callbackToken() == null ? "" : s.callbackToken())
                    .body(new Callback(s.solicitudId(), r.rutas(), r.sinAsignar()))
                    .retrieve().toBodilessEntity();
            log.info("Solicitud {}: {} rutas, {} sin asignar → callback OK", s.solicitudId(), r.rutas().size(), r.sinAsignar().size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log.error("No se pudo notificar el callback de {}: {}", s.solicitudId(), e.getMessage());
        }
    }
}
