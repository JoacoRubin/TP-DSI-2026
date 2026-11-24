package ar.utn.donatrack.logistica;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.rutas.domain.Parada;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regresión: GET /api/rutas/{id} no debe exponer patente, posición ni recorrido a usuarios ajenos. */
class AccesoARutaTest {

    private final UUID donante = UUID.randomUUID();
    private final Entrega entrega = DominioLogisticaTest.entrega(donante);
    private final Ruta ruta = Ruta.planificar(UUID.randomUUID(), LocalDate.of(2026, 11, 17), DominioLogisticaTest.camion(),
            List.of(Parada.de(1, entrega.entidadId(), entrega.destino(), List.of(entrega))), DominioLogisticaTest.AHORA);

    @Test
    void adminYChoferDelCamionAcceden() {
        assertThatCode(() -> new Actor("admin", "ADMIN", null).exigirAccesoA(ruta)).doesNotThrowAnyException();
        assertThatCode(() -> new Actor("chofer", "CHOFER", "AE123BC").exigirAccesoA(ruta)).doesNotThrowAnyException();
    }

    @Test
    void donanteYEntidadInvolucradosAcceden() {
        assertThatCode(() -> new Actor("ana", "DONANTE", donante.toString()).exigirAccesoA(ruta)).doesNotThrowAnyException();
        assertThatCode(() -> new Actor("escuela", "BENEFICIARIA", entrega.entidadId().toString()).exigirAccesoA(ruta))
                .doesNotThrowAnyException();
    }

    @Test
    void usuariosAjenosNoAcceden() {
        assertThatThrownBy(() -> new Actor("otro", "DONANTE", UUID.randomUUID().toString()).exigirAccesoA(ruta))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> new Actor("otra", "BENEFICIARIA", UUID.randomUUID().toString()).exigirAccesoA(ruta))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> new Actor("chofer", "CHOFER", "ZZ999ZZ").exigirAccesoA(ruta))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> new Actor("raro", "DONANTE", "no-es-uuid").exigirAccesoA(ruta))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> new Actor("anonimo", "ANONIMO", null).exigirAccesoA(ruta))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }
}
