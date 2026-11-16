package ar.utn.donatrack.planificador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * SIMULACIÓN del componente externo de planificación de rutas (no es parte de DonaTrack).
 * Existe sólo para poder probar la integración de punta a punta en local.
 */
@SpringBootApplication
@EnableAsync
public class PlanificadorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanificadorApplication.class, args);
    }
}
