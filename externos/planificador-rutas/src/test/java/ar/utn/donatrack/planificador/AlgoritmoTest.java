package ar.utn.donatrack.planificador;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlgoritmoTest {

    private final Algoritmo.Punto deposito = new Algoritmo.Punto(-34.5986, -58.4202);

    @Test
    void respetaLaCapacidadYDevuelveLoQueNoEntra() {
        Algoritmo.Camion chico = new Algoritmo.Camion(UUID.randomUUID(), "AE123BC", 100, 10);
        UUID destino = UUID.randomUUID();
        Algoritmo.Entrega e1 = new Algoritmo.Entrega(UUID.randomUUID(), destino, -34.6, -58.4, 80, 1);
        Algoritmo.Entrega e2 = new Algoritmo.Entrega(UUID.randomUUID(), destino, -34.6, -58.4, 50, 1);

        Algoritmo.Resultado r = Algoritmo.planificar(deposito, List.of(chico), List.of(e1, e2));

        assertThat(r.rutas()).hasSize(1);
        assertThat(r.sinAsignar()).containsExactly(e2.id());
    }

    @Test
    void ordenaLasParadasPorCercaniaDesdeElDeposito() {
        Algoritmo.Camion c = new Algoritmo.Camion(UUID.randomUUID(), "AE123BC", 1000, 50);
        Algoritmo.Entrega lejos = new Algoritmo.Entrega(UUID.randomUUID(), UUID.randomUUID(), -35.05, -58.76, 1, 0.1);
        Algoritmo.Entrega cerca = new Algoritmo.Entrega(UUID.randomUUID(), UUID.randomUUID(), -34.61, -58.43, 1, 0.1);

        Algoritmo.Resultado r = Algoritmo.planificar(deposito, List.of(c), List.of(lejos, cerca));

        assertThat(r.rutas().get(0).paradas()).extracting(p -> p.entregaIds().get(0)).containsExactly(cerca.id(), lejos.id());
    }
}
