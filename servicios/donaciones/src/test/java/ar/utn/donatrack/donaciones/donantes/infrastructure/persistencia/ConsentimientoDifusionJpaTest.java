package ar.utn.donatrack.donaciones.donantes.infrastructure.persistencia;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.AHORA;
import static ar.utn.donatrack.donaciones.Fixtures.direccion;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DonanteRepositorio.class))
@ActiveProfiles("test")
class ConsentimientoDifusionJpaTest {

    @Autowired
    DonanteRepositorio donantes;
    @Autowired
    EntityManager em;

    private static PersonaHumana persona() {
        return PersonaHumana.registrar(UUID.randomUUID(), "Ana", "Pérez", 30, new Documento(TipoDocumento.DNI, "12345678"),
                Genero.FEMENINO, direccion(), new MediosContacto(UUID.randomUUID() + "@mail.com", null, null, MedioNotificacion.EMAIL), AHORA);
    }

    @Test
    void elConsentimientoSePersiste() {
        PersonaHumana ana = persona();
        ana.aceptarDifusionLogros(true);
        donantes.guardar(ana);
        em.flush();
        em.clear();

        assertThat(((PersonaHumana) donantes.buscar(ana.id()).orElseThrow()).aceptaDifusionLogros()).isTrue();
    }

    @Test
    void losDonantesAnterioresAlCambioQuedanSinConsentimiento() {
        PersonaHumana ana = persona();
        donantes.guardar(ana);
        em.flush();
        // fila creada antes de que existiera la columna: queda en NULL
        em.createNativeQuery("update donantes set acepta_difusion_logros = null where id = ?1")
                .setParameter(1, ana.id()).executeUpdate();
        em.clear();

        assertThat(((PersonaHumana) donantes.buscar(ana.id()).orElseThrow()).aceptaDifusionLogros()).isFalse();
    }
}
