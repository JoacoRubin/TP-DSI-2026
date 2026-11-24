package ar.utn.donatrack.donaciones.compartido.infrastructure.outbox;

import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La garantía central del outbox: el comando se confirma o se descarta JUNTO con la transacción local. */
@DataJpaTest
@ActiveProfiles("test")
@Import(BandejaSalidaJpa.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxTransaccionalJpaTest {

    @Autowired
    BandejaSalida bandeja;
    @Autowired
    MensajesSalientesJpa jpa;
    @Autowired
    PlatformTransactionManager tm;

    ServicioLogistica puerto;
    TransactionTemplate tx;

    @BeforeEach
    void preparar() {
        jpa.deleteAll();
        puerto = new ServicioLogisticaConOutbox(bandeja, new ObjectMapper(), Clock.systemUTC());
        tx = new TransactionTemplate(tm);
    }

    @Test
    void siLaTransaccionHaceRollbackElComandoNoQuedaRegistrado() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            puerto.cancelarEntrega(UUID.randomUUID());
            throw new IllegalStateException("falla posterior del caso de uso");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(bandeja.pendientes(10)).isEmpty();
    }

    @Test
    void siLaTransaccionConfirmaElComandoQuedaPendienteEnOrden() {
        UUID primera = UUID.randomUUID();
        UUID segunda = UUID.randomUUID();
        tx.executeWithoutResult(s -> {
            puerto.cancelarEntrega(primera);
            puerto.cancelarEntrega(segunda);
        });

        assertThat(bandeja.pendientes(10)).extracting(MensajeSaliente::clave)
                .containsExactly(primera.toString(), segunda.toString());
    }
}
