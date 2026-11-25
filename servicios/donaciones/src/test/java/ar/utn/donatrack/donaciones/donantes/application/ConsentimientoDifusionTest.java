package ar.utn.donatrack.donaciones.donantes.application;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.EnMemoria;
import ar.utn.donatrack.donaciones.compartido.application.AltaDeCuenta;
import ar.utn.donatrack.donaciones.compartido.application.Enlaces;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.AHORA;
import static ar.utn.donatrack.donaciones.Fixtures.direccion;
import static org.assertj.core.api.Assertions.assertThat;

/** Ley 25.326: la difusión pública de logros es opt-in y el consentimiento queda registrado en el alta. */
class ConsentimientoDifusionTest {

    private final EnMemoria.Donantes donantes = new EnMemoria.Donantes();
    private final RegistrarPersonaHumana registrar = new RegistrarPersonaHumana(donantes,
            new AltaDeCuenta(new EnMemoria.CuentasFalsas(), new EnMemoria.NotificadorFalso(), new Enlaces("http://front")),
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private static RegistrarPersonaHumana.Comando lucia(boolean aceptaDifusion) {
        return new RegistrarPersonaHumana.Comando("Lucía", "Gómez", 28, new Documento(TipoDocumento.DNI, "35123456"),
                Genero.FEMENINO, direccion(),
                new MediosContacto("lucia-" + UUID.randomUUID() + "@mail.com", null, null, MedioNotificacion.EMAIL),
                "Segura123!", aceptaDifusion);
    }

    @Test
    void porDefectoNoSeAceptaLaDifusion() {
        PersonaHumana ana = PersonaHumana.registrar(UUID.randomUUID(), "Ana", "Pérez", 34,
                new Documento(TipoDocumento.DNI, "30123456"), Genero.FEMENINO, direccion(),
                new MediosContacto("ana@mail.com", null, null, MedioNotificacion.EMAIL), AHORA);

        assertThat(ana.aceptaDifusionLogros()).isFalse();
    }

    @Test
    void elAltaRegistraLoQueEligioLaPersona() {
        assertThat(registrar.ejecutar(lucia(true)).aceptaDifusionLogros()).isTrue();
        assertThat(registrar.ejecutar(lucia(false)).aceptaDifusionLogros()).isFalse();
    }
}
