package ar.utn.donatrack.incentivos.infrastructure.outbox;

import ar.utn.donatrack.contratos.Colas;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La garantía central: el aviso se confirma o se descarta JUNTO con la insignia que lo origina. */
@DataJpaTest
@Import(BandejaSalidaJpa.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxIncentivosJpaTest {

    @Autowired
    BandejaSalida bandeja;
    @Autowired
    MensajesSalientesJpa jpa;
    @Autowired
    PlatformTransactionManager tm;

    PublicadorColasConOutbox colas;
    TransactionTemplate tx;

    @BeforeEach
    void preparar() {
        jpa.deleteAll();
        colas = new PublicadorColasConOutbox(bandeja, JsonMapper.builder().findAndAddModules().build(), Clock.systemUTC());
        tx = new TransactionTemplate(tm);
    }

    @Test
    void siLaTransaccionHaceRollbackElAvisoNoSale() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            colas.publicar(Colas.NOTIFICACIONES, OutboxIncentivosTest.misionCumplida("¡Misión cumplida!"));
            throw new IllegalStateException("falla posterior al otorgar la insignia");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(bandeja.pendientes(10)).isEmpty();
    }

    @Test
    void siLaTransaccionConfirmaLosAvisosQuedanPendientesEnOrden() {
        tx.executeWithoutResult(s -> {
            colas.publicar(Colas.NOTIFICACIONES, OutboxIncentivosTest.misionCumplida("primero"));
            colas.publicar(Colas.NOTIFICACIONES, OutboxIncentivosTest.misionCumplida("segundo"));
        });

        assertThat(bandeja.pendientes(10)).extracting(MensajeSaliente::payload)
                .satisfiesExactly(p -> assertThat(p).contains("primero"), p -> assertThat(p).contains("segundo"));
    }
}
