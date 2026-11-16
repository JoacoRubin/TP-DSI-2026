package ar.utn.donatrack.donaciones.importacion.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import ar.utn.donatrack.donaciones.importacion.domain.ImportacionRepositorio;

import java.util.List;
import java.util.UUID;

public class ConsultasImportacion {

    private final ImportacionRepositorio importaciones;

    public ConsultasImportacion(ImportacionRepositorio importaciones) {
        this.importaciones = importaciones;
    }

    public Importacion detalle(UUID id) {
        return importaciones.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Importación", id));
    }

    public List<Importacion> recientes() {
        return importaciones.recientes(20);
    }
}
