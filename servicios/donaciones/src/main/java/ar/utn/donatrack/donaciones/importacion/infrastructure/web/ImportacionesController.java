package ar.utn.donatrack.donaciones.importacion.infrastructure.web;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.importacion.application.ConsultasImportacion;
import ar.utn.donatrack.donaciones.importacion.application.IniciarImportacion;
import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
class ImportacionesController {

    private final IniciarImportacion iniciar;
    private final ConsultasImportacion consultas;
    private final ProcesadorImportacionAsincronico procesador;

    ImportacionesController(IniciarImportacion iniciar, ConsultasImportacion consultas,
                            ProcesadorImportacionAsincronico procesador) {
        this.iniciar = iniciar;
        this.consultas = consultas;
        this.procesador = procesador;
    }

    record ErrorVista(int linea, String mensaje) {
    }

    record ImportacionVista(UUID id, String archivo, String estado, int filasProcesadas, int creados, int actualizados,
                            int conError, List<ErrorVista> errores, Instant iniciadaEn, Instant finalizadaEn,
                            String detalleFallo) {
        static ImportacionVista de(Importacion i) {
            return new ImportacionVista(i.id(), i.archivo(), i.estado().name(), i.filasProcesadas(), i.creados(),
                    i.actualizados(), i.conError(), i.errores().stream().map(e -> new ErrorVista(e.linea(), e.mensaje())).toList(),
                    i.iniciadaEn(), i.finalizadaEn(), i.detalleFallo());
        }
    }

    /**
     * El archivo se copia a disco y se procesa en segundo plano: la request responde 202 al
     * instante y el progreso se consulta por GET (patrón "asynchronous request-reply").
     */
    @PostMapping("/api/importaciones")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ImportacionVista importar(@RequestPart("archivo") MultipartFile archivo) throws IOException {
        if (archivo.isEmpty()) {
            throw new ReglaDeNegocioException("El archivo está vacío");
        }
        String nombre = archivo.getOriginalFilename() == null ? "donantes.csv" : archivo.getOriginalFilename();
        if (!nombre.toLowerCase().endsWith(".csv")) {
            throw new ReglaDeNegocioException("El archivo debe ser .csv");
        }
        Path temporal = Files.createTempFile("donatrack-importacion-", ".csv");
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, temporal, StandardCopyOption.REPLACE_EXISTING);
        }
        Importacion imp = iniciar.ejecutar(nombre);
        procesador.procesar(imp.id(), temporal);
        return ImportacionVista.de(imp);
    }

    @GetMapping("/api/importaciones/{id}")
    ImportacionVista detalle(@PathVariable UUID id) {
        return ImportacionVista.de(consultas.detalle(id));
    }

    @GetMapping("/api/importaciones")
    List<ImportacionVista> recientes() {
        return consultas.recientes().stream().map(ImportacionVista::de).toList();
    }
}
