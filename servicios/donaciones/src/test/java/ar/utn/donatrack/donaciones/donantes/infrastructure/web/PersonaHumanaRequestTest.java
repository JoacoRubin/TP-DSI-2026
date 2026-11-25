package ar.utn.donatrack.donaciones.donantes.infrastructure.web;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.DtosComunes.DireccionDto;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El consentimiento es personal: sólo vale el que da la persona al autorregistrarse. En el alta que hace
 * la administración en el depósito queda en "no", aunque el cuerpo diga otra cosa.
 */
class PersonaHumanaRequestTest {

    private static DonantesController.PersonaHumanaRequest lucia(Boolean aceptaDifusion) {
        return new DonantesController.PersonaHumanaRequest("Lucía", "Gómez", 28, TipoDocumento.DNI, "35123456",
                Genero.FEMENINO, new DireccionDto("Medrano", "951", "CABA", "Buenos Aires", "C1179AAQ", null, null),
                new DonantesController.ContactoDto("lucia@mail.com", null, null, MedioNotificacion.EMAIL),
                "Segura123!", aceptaDifusion);
    }

    @Test
    void enElAutorregistroValeLoQueEligioLaPersona() {
        assertThat(lucia(true).comandoAutorregistro().aceptaDifusionLogros()).isTrue();
        assertThat(lucia(false).comandoAutorregistro().aceptaDifusionLogros()).isFalse();
        assertThat(lucia(null).comandoAutorregistro().aceptaDifusionLogros()).isFalse();
    }

    @Test
    void laAdministracionNoPuedeConsentirPorLaPersona() {
        assertThat(lucia(true).comandoAdministracion().aceptaDifusionLogros()).isFalse();
        assertThat(lucia(true).comandoAdministracion().clave()).isNull();
    }
}
