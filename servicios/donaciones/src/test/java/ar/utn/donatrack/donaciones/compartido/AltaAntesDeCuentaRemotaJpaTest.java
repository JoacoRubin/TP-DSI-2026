package ar.utn.donatrack.donaciones.compartido;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.AHORA;
import static ar.utn.donatrack.donaciones.Fixtures.direccion;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las altas guardan localmente y DESPUÉS crean la cuenta en Autenticación (que no es
 * idempotente). Para no dejar cuentas huérfanas, un error de la base local (p. ej. email
 * duplicado) tiene que aparecer en {@code guardar}, ANTES de la llamada remota, y no recién en el commit.
 */
@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {DonanteRepositorio.class, EntidadRepositorio.class}))
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AltaAntesDeCuentaRemotaJpaTest {

    @Autowired
    DonanteRepositorio donantes;
    @Autowired
    EntidadRepositorio entidades;
    @Autowired
    PlatformTransactionManager tm;

    private static PersonaHumana persona(String email) {
        return PersonaHumana.registrar(UUID.randomUUID(), "Ana", "Pérez", 30, new Documento(TipoDocumento.DNI, "12345678"),
                Genero.FEMENINO, direccion(), new MediosContacto(email, null, null, MedioNotificacion.EMAIL), AHORA);
    }

    private static EntidadBeneficiaria entidad(String email) {
        return EntidadBeneficiaria.registrar(UUID.randomUUID(), "Escuela 12", direccion(), "+54 11 4444-4444", List.of(email), AHORA);
    }

    @Test
    void guardarDonanteDetectaElEmailDuplicadoEnElMomento() {
        String email = "dup-" + UUID.randomUUID() + "@mail.com";
        new TransactionTemplate(tm).executeWithoutResult(s -> donantes.guardar(persona(email)));

        new TransactionTemplate(tm).executeWithoutResult(s -> {
            assertThatThrownBy(() -> donantes.guardar(persona(email))).isInstanceOf(DataIntegrityViolationException.class);
            s.setRollbackOnly();
        });
    }

    @Test
    void guardarEntidadDetectaElEmailDuplicadoEnElMomento() {
        String email = "dup-" + UUID.randomUUID() + "@mail.com";
        new TransactionTemplate(tm).executeWithoutResult(s -> entidades.guardar(entidad(email)));

        new TransactionTemplate(tm).executeWithoutResult(s -> {
            assertThatThrownBy(() -> entidades.guardar(entidad(email))).isInstanceOf(DataIntegrityViolationException.class);
            s.setRollbackOnly();
        });
    }
}
