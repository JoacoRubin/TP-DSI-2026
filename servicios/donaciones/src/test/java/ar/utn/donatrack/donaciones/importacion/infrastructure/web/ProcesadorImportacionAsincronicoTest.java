package ar.utn.donatrack.donaciones.importacion.infrastructure.web;

import ar.utn.donatrack.donaciones.EnMemoria;
import ar.utn.donatrack.donaciones.compartido.application.AltaDeCuenta;
import ar.utn.donatrack.donaciones.compartido.application.Enlaces;
import ar.utn.donatrack.donaciones.importacion.application.FallarImportacion;
import ar.utn.donatrack.donaciones.importacion.application.IniciarImportacion;
import ar.utn.donatrack.donaciones.importacion.application.ProcesarImportacion;
import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;

import static ar.utn.donatrack.donaciones.Fixtures.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

/** Hallazgo de auditoría: si el CSV no se podía abrir, la importación quedaba EN_CURSO indefinidamente. */
class ProcesadorImportacionAsincronicoTest {

    @TempDir
    Path tmp;

    @Test
    void siElArchivoNoSePuedeLeerLaImportacionTerminaFallida() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        EnMemoria.Importaciones importaciones = new EnMemoria.Importaciones();
        EnMemoria.CuentasFalsas cuentas = new EnMemoria.CuentasFalsas();
        ProcesarImportacion procesar = new ProcesarImportacion(importaciones, new EnMemoria.Donantes(), cuentas,
                new AltaDeCuenta(cuentas, new EnMemoria.NotificadorFalso(), new Enlaces("http://front")),
                EnMemoria.SIN_TRANSACCION, reloj);
        Importacion imp = new IniciarImportacion(importaciones, reloj).ejecutar("donantes.csv");

        new ProcesadorImportacionAsincronico(procesar, new FallarImportacion(importaciones, reloj))
                .procesar(imp.id(), tmp.resolve("no-existe.csv"));

        Importacion r = importaciones.buscar(imp.id()).orElseThrow();
        assertThat(r.estado()).isEqualTo(Importacion.Estado.FALLIDA);
        assertThat(r.detalleFallo()).contains("No se pudo leer el archivo");
    }
}
