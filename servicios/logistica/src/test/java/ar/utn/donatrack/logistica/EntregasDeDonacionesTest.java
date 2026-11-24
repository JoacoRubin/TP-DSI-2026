package ar.utn.donatrack.logistica;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.rutas.application.ConsultasLogistica;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pedir entregas por id de donación no debe saltear la restricción por rol. */
class EntregasDeDonacionesTest {

    private final CasosDeUsoLogisticaTest.Entregas entregas = new CasosDeUsoLogisticaTest.Entregas();
    private final ConsultasLogistica consultas = new ConsultasLogistica(null, entregas, null, Clock.systemUTC());
    private final Entrega propia = DominioLogisticaTest.entrega(UUID.randomUUID());
    private final Entrega ajena = DominioLogisticaTest.entrega(UUID.randomUUID());

    {
        entregas.guardar(propia);
        entregas.guardar(ajena);
    }

    private Set<UUID> ambas() {
        return Set.of(propia.donacionId(), ajena.donacionId());
    }

    @Test
    void laEntidadSoloRecibeLasSuyasAunquePidaIdsAjenos() {
        Actor escuela = new Actor("escuela", "BENEFICIARIA", propia.entidadId().toString());
        assertThat(consultas.entregasDeDonaciones(ambas(), escuela)).containsExactly(propia);
    }

    @Test
    void elDonanteSoloRecibeLasSuyas() {
        Actor ana = new Actor("ana", "DONANTE", ajena.donanteId().toString());
        assertThat(consultas.entregasDeDonaciones(ambas(), ana)).containsExactly(ajena);
    }

    @Test
    void elAdminRecibeTodasYOtrosRolesNinguna() {
        assertThat(consultas.entregasDeDonaciones(ambas(), new Actor("admin", "ADMIN", null))).hasSize(2);
        assertThat(consultas.entregasDeDonaciones(ambas(), new Actor("chofer", "CHOFER", "AE123BC"))).isEmpty();
    }

    @Test
    void acotaLaCantidadDeIdsPorConsulta() {
        List<UUID> muchos = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();
        assertThatThrownBy(() -> consultas.entregasDeDonaciones(Set.copyOf(muchos), new Actor("admin", "ADMIN", null)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
