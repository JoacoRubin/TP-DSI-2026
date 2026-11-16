package ar.utn.donatrack.donaciones.donantes.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DonanteTest {

    private PersonaHumana ana() {
        return PersonaHumana.registrar(UUID.randomUUID(), "Ana", "Pérez", 34, new Documento(TipoDocumento.DNI, "12.345.678"),
                Genero.FEMENINO, direccion(), new MediosContacto("ana@mail.com", null, "+54 9 11 5555-5555", MedioNotificacion.WHATSAPP), AHORA);
    }

    @Test
    void elEmailEsObligatorioComoMedioDeContacto() {
        assertThatThrownBy(() -> new MediosContacto(null, "+54 11 1234-5678", null, MedioNotificacion.SMS))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void noSePuedePreferirUnMedioSinDatos() {
        assertThatThrownBy(() -> new MediosContacto("a@mail.com", null, null, MedioNotificacion.SMS))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("SMS");
    }

    @Test
    void elDocumentoSeNormalizaYSeValidaElFormato() {
        assertThat(new Documento(TipoDocumento.DNI, "12.345.678").numero()).isEqualTo("12345678");
        assertThat(new Documento(TipoDocumento.CUIT, "30-12345678-9").numero()).isEqualTo("30123456789");
        assertThatThrownBy(() -> new Documento(TipoDocumento.CUIT, "123")).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void unaPersonaHumanaRegistradaPorFormularioExigeTodosLosDatos() {
        assertThatThrownBy(() -> PersonaHumana.registrar(UUID.randomUUID(), "Ana", "Pérez", null,
                new Documento(TipoDocumento.DNI, "12345678"), Genero.FEMENINO, direccion(),
                new MediosContacto("ana@mail.com", null, null, null), AHORA))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("edad");
    }

    @Test
    void elDestinatarioRespetaElMedioPreferido() {
        assertThat(ana().comoDestinatario().medioPreferido()).isEqualTo(MedioNotificacion.WHATSAPP);
        assertThat(ana().nombreVisible()).isEqualTo("Ana Pérez");
    }

    @Test
    void seLeAvisaUnaSolaVezPorPeriodoDeInactividad() {
        PersonaHumana ana = ana();
        Duration umbral = Duration.ofDays(20);
        assertThat(ana.requiereAvisoInactividad(AHORA.plus(Duration.ofDays(10)), umbral)).isFalse();
        assertThat(ana.requiereAvisoInactividad(AHORA.plus(Duration.ofDays(21)), umbral)).isTrue();

        ana.registrarAvisoInactividad(AHORA.plus(Duration.ofDays(21)));
        assertThat(ana.requiereAvisoInactividad(AHORA.plus(Duration.ofDays(30)), umbral)).isFalse();

        ana.registrarInteraccion(AHORA.plus(Duration.ofDays(31)));
        assertThat(ana.requiereAvisoInactividad(AHORA.plus(Duration.ofDays(52)), umbral)).isTrue();
    }

    @Test
    void unaPersonaJuridicaTieneRepresentantes() {
        PersonaJuridica arcos = PersonaJuridica.registrar(UUID.randomUUID(), "Arcos Plateados S.A.", TipoPersonaJuridica.EMPRESA,
                "Gastronomía", new Documento(TipoDocumento.CUIT, "30-12345678-9"), direccion(),
                new MediosContacto("contacto@arcos.com", null, null, null),
                List.of(new Representante("Laura Gómez", "laura@arcos.com")), AHORA);
        assertThat(arcos.nombreVisible()).isEqualTo("Arcos Plateados S.A.");
        assertThat(arcos.tipo()).isEqualTo(TipoDonante.JURIDICA);
        assertThat(arcos.representantes()).hasSize(1);
    }
}
