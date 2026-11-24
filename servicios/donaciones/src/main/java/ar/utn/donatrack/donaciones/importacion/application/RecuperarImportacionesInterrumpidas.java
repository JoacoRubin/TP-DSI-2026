package ar.utn.donatrack.donaciones.importacion.application;

import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import ar.utn.donatrack.donaciones.importacion.domain.ImportacionRepositorio;

import java.time.Clock;
import java.util.List;

/**
 * Al arrancar el servicio: el procesamiento es en memoria (asíncrono), así que una importación
 * EN_CURSO al iniciar quedó interrumpida por un reinicio/caída y nadie la va a terminar. Se cierra
 * como FALLIDA indicando que se vuelva a subir (es seguro: la importación es idempotente por email).
 * Supone una única instancia de Donaciones procesando importaciones.
 */
public class RecuperarImportacionesInterrumpidas {

    static final String MOTIVO = "Interrumpida: el servicio se reinició durante el proceso. Volvé a subir el archivo "
            + "(las filas ya importadas se actualizan, no se duplican).";

    private final ImportacionRepositorio importaciones;
    private final Clock reloj;

    public RecuperarImportacionesInterrumpidas(ImportacionRepositorio importaciones, Clock reloj) {
        this.importaciones = importaciones;
        this.reloj = reloj;
    }

    /** @return cantidad de importaciones cerradas */
    public int ejecutar() {
        List<Importacion> interrumpidas = importaciones.enCurso();
        interrumpidas.forEach(i -> {
            i.fallar(MOTIVO, reloj.instant());
            importaciones.guardar(i);
        });
        return interrumpidas.size();
    }
}
