package ar.utn.donatrack.donaciones.importacion.application;

import ar.utn.donatrack.donaciones.EnMemoria;
import ar.utn.donatrack.donaciones.compartido.application.AltaDeCuenta;
import ar.utn.donatrack.donaciones.compartido.application.Enlaces;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static ar.utn.donatrack.donaciones.Fixtures.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class ProcesarImportacionTest {

    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final EnMemoria.Importaciones importaciones = new EnMemoria.Importaciones();
    private final EnMemoria.Donantes donantes = new EnMemoria.Donantes();
    private final EnMemoria.CuentasFalsas cuentas = new EnMemoria.CuentasFalsas();
    private final EnMemoria.NotificadorFalso notificador = new EnMemoria.NotificadorFalso();

    private Importacion importar(List<String> lineas) {
        Importacion imp = new IniciarImportacion(importaciones, reloj).ejecutar("donantes.csv");
        return new ProcesarImportacion(importaciones, donantes, cuentas,
                new AltaDeCuenta(cuentas, notificador, new Enlaces("http://front")), EnMemoria.SIN_TRANSACCION, reloj)
                .ejecutar(imp.id(), lineas.iterator());
    }

    @Test
    void creaDonantesNuevosConCuentaPendienteYLesEnviaElEnlaceDeActivacion() {
        Importacion r = importar(List.of(
                "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono",
                "HUMANA,DNI,12345678,Ana Pérez,ana@mail.com,+54 11 5555-5555",
                "JURIDICA,CUIT,30-12345678-9,Arcos Plateados S.A.,contacto@empresa.com,+54 11 4444-4444"));

        assertThat(r.estado()).isEqualTo(Importacion.Estado.FINALIZADA);
        assertThat(r.creados()).isEqualTo(2);
        assertThat(donantes.datos).hasSize(2);
        assertThat(notificador.deTipo("BIENVENIDA")).hasSize(2)
                .allSatisfy(n -> assertThat(n.mensaje()).contains("http://front/activar?token=tok-"));
    }

    @Test
    void siElEmailYaExisteActualizaEnLugarDeDuplicar() {
        importar(List.of("TipoPersona,TipoDoc,Documento,Nombre,Email,Telefono",
                "HUMANA,DNI,12345678,Ana Pérez,ana@mail.com,"));
        Importacion r = importar(List.of("TipoPersona,TipoDoc,Documento,Nombre,Email,Telefono",
                "HUMANA,DNI,12345678,Ana María Pérez,ana@mail.com,+54 11 1111-1111"));

        assertThat(r.actualizados()).isEqualTo(1);
        assertThat(r.creados()).isZero();
        assertThat(donantes.datos).hasSize(1);
        Donante ana = donantes.datos.values().iterator().next();
        assertThat(((PersonaHumana) ana).nombre()).isEqualTo("Ana María");
        assertThat(ana.contacto().telefono()).isEqualTo("+54 11 1111-1111");
        assertThat(notificador.deTipo("BIENVENIDA")).hasSize(1);
    }

    @Test
    void lasFilasInvalidasSeReportanConSuLineaYNoFrenanElProceso() {
        Importacion r = importar(List.of("TipoPersona,TipoDoc,Documento,Nombre,Email,Telefono",
                "HUMANA,DNI,12345678,Ana Pérez,ana@mail.com,",
                "HUMANA,DNI,12345678,Sin Email,,",
                "HUMANA,CUIT,1,Doc Malo,doc@mail.com,",
                "JURIDICA,CUIT,30-12345678-9,Org,org@mail.com,"));

        assertThat(r.creados()).isEqualTo(2);
        assertThat(r.conError()).isEqualTo(2);
        assertThat(r.errores()).extracting(Importacion.ErrorFila::linea).containsExactlyInAnyOrder(3, 4);
    }

    @Test
    void veinteMilFilasSeProcesanEnLotesSinConsultasFilaPorFila() {
        List<String> lineas = new ArrayList<>();
        lineas.add("TipoPersona,TipoDoc,Documento,Nombre,Email,Telefono");
        for (int i = 0; i < 20_000; i++) {
            lineas.add("HUMANA,DNI," + (10_000_000 + i) + ",Persona " + i + ",persona" + i + "@mail.com,");
        }

        Importacion r = importar(lineas);

        assertThat(r.creados()).isEqualTo(20_000);
        assertThat(r.filasProcesadas()).isEqualTo(20_000);
        int lotes = 20_000 / ProcesarImportacion.TAMANIO_LOTE;
        assertThat(donantes.consultasPorEmails).isEqualTo(lotes);
        assertThat(cuentas.llamadasLote).isEqualTo(lotes);
    }

    @Test
    void unArchivoConEncabezadoInvalidoFallaConMensajeClaro() {
        Importacion r = importar(List.of("nombre;apellido", "x;y"));
        assertThat(r.estado()).isEqualTo(Importacion.Estado.FALLIDA);
        assertThat(r.detalleFallo()).contains("Encabezado");
    }
}
