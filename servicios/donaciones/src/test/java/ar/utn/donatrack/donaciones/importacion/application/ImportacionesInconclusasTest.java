package ar.utn.donatrack.donaciones.importacion.application;

import ar.utn.donatrack.donaciones.EnMemoria;
import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static ar.utn.donatrack.donaciones.Fixtures.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

/** Regresión: ninguna importación puede quedar EN_CURSO para siempre. */
class ImportacionesInconclusasTest {

    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final EnMemoria.Importaciones importaciones = new EnMemoria.Importaciones();

    private Importacion iniciada() {
        return new IniciarImportacion(importaciones, reloj).ejecutar("donantes.csv");
    }

    @Test
    void unaImportacionQueNoPudoProcesarseQuedaFallidaConElMotivo() {
        Importacion imp = iniciada();

        new FallarImportacion(importaciones, reloj).ejecutar(imp.id(), "No se pudo leer el archivo");

        Importacion r = importaciones.buscar(imp.id()).orElseThrow();
        assertThat(r.estado()).isEqualTo(Importacion.Estado.FALLIDA);
        assertThat(r.detalleFallo()).isEqualTo("No se pudo leer el archivo");
        assertThat(r.finalizadaEn()).isEqualTo(AHORA);
    }

    @Test
    void fallarNoPisaUnaImportacionYaTerminada() {
        Importacion imp = iniciada();
        imp.finalizar(AHORA);
        importaciones.guardar(imp);

        new FallarImportacion(importaciones, reloj).ejecutar(imp.id(), "tarde");

        assertThat(importaciones.buscar(imp.id()).orElseThrow().estado()).isEqualTo(Importacion.Estado.FINALIZADA);
    }

    @Test
    void alArrancarSeCierranLasQueElReinicioDejoEnCurso() {
        Importacion interrumpida = iniciada();
        Importacion terminada = iniciada();
        terminada.finalizar(AHORA);
        importaciones.guardar(terminada);

        int cerradas = new RecuperarImportacionesInterrumpidas(importaciones, reloj).ejecutar();

        assertThat(cerradas).isEqualTo(1);
        Importacion r = importaciones.buscar(interrumpida.id()).orElseThrow();
        assertThat(r.estado()).isEqualTo(Importacion.Estado.FALLIDA);
        assertThat(r.detalleFallo()).contains("reinició").contains("Volvé a subir");
        assertThat(importaciones.buscar(terminada.id()).orElseThrow().estado()).isEqualTo(Importacion.Estado.FINALIZADA);
    }
}
