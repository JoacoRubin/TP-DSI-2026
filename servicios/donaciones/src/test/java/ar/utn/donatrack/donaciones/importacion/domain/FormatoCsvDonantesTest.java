package ar.utn.donatrack.donaciones.importacion.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDonante;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormatoCsvDonantesTest {

    private final FormatoCsvDonantes formato =
            FormatoCsvDonantes.desdeEncabezado("TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono");

    @Test
    void parseaUnaPersonaHumana() {
        FilaDonante f = formato.parsear("HUMANA,DNI,12345678,Ana Pérez,ana@mail.com,+54 11 5555-5555", 2);
        assertThat(f.tipo()).isEqualTo(TipoDonante.HUMANA);
        assertThat(f.tipoDocumento()).isEqualTo(TipoDocumento.DNI);
        assertThat(f.nombre()).isEqualTo("Ana Pérez");
        assertThat(f.email()).isEqualTo("ana@mail.com");
    }

    @Test
    void soportaCamposEntreComillasConComasYPuntoYComaComoSeparador() {
        FilaDonante f = formato.parsear("JURIDICA,CUIT,30-12345678-9,\"Arcos Plateados, S.A.\",contacto@empresa.com,", 3);
        assertThat(f.nombre()).isEqualTo("Arcos Plateados, S.A.");
        assertThat(f.telefono()).isNull();

        FormatoCsvDonantes conPuntoYComa = FormatoCsvDonantes.desdeEncabezado("TipoPersona;TipoDoc;Documento;Nombre;Email;Telefono");
        assertThat(conPuntoYComa.parsear("HUMANA;DNI;1234567;Beto;b@mail.com;1", 2).nombre()).isEqualTo("Beto");
    }

    @Test
    void informaLaLineaCuandoUnaFilaEsInvalida() {
        assertThatThrownBy(() -> formato.parsear("MARCIANA,DNI,1,X,x@mail.com,", 7))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("7");
        assertThatThrownBy(() -> formato.parsear("HUMANA,DNI,123", 8))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void separaNombreYApellidoDeUnaPersonaHumana() {
        FilaDonante f = formato.parsear("HUMANA,DNI,12345678,María José García,mj@mail.com,", 2);
        assertThat(f.nombreDePila()).isEqualTo("María José");
        assertThat(f.apellido()).isEqualTo("García");
    }
}
