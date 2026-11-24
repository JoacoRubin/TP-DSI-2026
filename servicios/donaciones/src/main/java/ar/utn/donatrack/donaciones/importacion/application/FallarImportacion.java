package ar.utn.donatrack.donaciones.importacion.application;

import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import ar.utn.donatrack.donaciones.importacion.domain.ImportacionRepositorio;

import java.time.Clock;
import java.util.UUID;

/**
 * Cierra como FALLIDA una importación que no llegó a procesarse (p. ej. el archivo no se pudo
 * leer). Sin esto quedaría EN_CURSO para siempre y la pantalla mostraría un progreso eterno.
 * No pisa una importación que ya terminó.
 */
public class FallarImportacion {

    private final ImportacionRepositorio importaciones;
    private final Clock reloj;

    public FallarImportacion(ImportacionRepositorio importaciones, Clock reloj) {
        this.importaciones = importaciones;
        this.reloj = reloj;
    }

    public void ejecutar(UUID importacionId, String motivo) {
        importaciones.buscar(importacionId)
                .filter(i -> i.estado() == Importacion.Estado.EN_CURSO)
                .ifPresent(i -> {
                    i.fallar(motivo, reloj.instant());
                    importaciones.guardar(i);
                });
    }
}
