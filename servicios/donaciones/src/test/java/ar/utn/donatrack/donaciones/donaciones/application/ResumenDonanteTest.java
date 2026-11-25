package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaJuridica;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import ar.utn.donatrack.donaciones.donantes.domain.TipoPersonaJuridica;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.AHORA;
import static ar.utn.donatrack.donaciones.Fixtures.direccion;
import static org.assertj.core.api.Assertions.assertThat;

/** El consentimiento viaja en el evento para que Incentivos cree el perfil respetándolo. */
class ResumenDonanteTest {

    private static PersonaHumana ana() {
        return PersonaHumana.registrar(UUID.randomUUID(), "Ana", "Pérez", 34, new Documento(TipoDocumento.DNI, "30123456"),
                Genero.FEMENINO, direccion(), new MediosContacto("ana@mail.com", null, null, MedioNotificacion.EMAIL), AHORA);
    }

    @Test
    void elResumenLlevaElConsentimientoDeDifusion() {
        PersonaHumana ana = ana();
        assertThat(ResumenDonante.de(ana).aceptaDifusionLogros()).isFalse();

        ana.aceptarDifusionLogros(true);
        assertThat(ResumenDonante.de(ana).aceptaDifusionLogros()).isTrue();
    }

    @Test
    void unaPersonaJuridicaNuncaSeDifunde() {
        PersonaJuridica arcos = PersonaJuridica.registrar(UUID.randomUUID(), "Arcos Plateados S.A.", TipoPersonaJuridica.EMPRESA,
                "Gastronomía", new Documento(TipoDocumento.CUIT, "30-12345678-9"), direccion(),
                new MediosContacto("contacto@arcos.com", null, null, null), List.of(), AHORA);

        assertThat(ResumenDonante.de(arcos).aceptaDifusionLogros()).isFalse();
    }
}
