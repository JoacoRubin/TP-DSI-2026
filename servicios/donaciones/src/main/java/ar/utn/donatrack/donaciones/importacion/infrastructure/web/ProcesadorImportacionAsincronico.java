package ar.utn.donatrack.donaciones.importacion.infrastructure.web;

import ar.utn.donatrack.donaciones.importacion.application.ProcesarImportacion;
import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Lee el CSV en streaming (línea a línea) y delega en el caso de uso. */
@Component
class ProcesadorImportacionAsincronico {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorImportacionAsincronico.class);
    private final ProcesarImportacion procesar;

    ProcesadorImportacionAsincronico(ProcesarImportacion procesar) {
        this.procesar = procesar;
    }

    @Async
    public void procesar(UUID importacionId, Path archivo) {
        long inicio = System.currentTimeMillis();
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            Importacion r = procesar.ejecutar(importacionId, lector.lines().iterator());
            log.info("Importación {} {}: {} creados, {} actualizados, {} con error en {} ms", importacionId, r.estado(),
                    r.creados(), r.actualizados(), r.conError(), System.currentTimeMillis() - inicio);
        } catch (IOException e) {
            log.error("No se pudo leer el archivo de importación {}", importacionId, e);
        } finally {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignorada) {
                // archivo temporal: el SO lo limpia
            }
        }
    }
}
