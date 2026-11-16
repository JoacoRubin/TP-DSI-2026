package ar.utn.donatrack.donaciones.importacion.application;

import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import ar.utn.donatrack.donaciones.importacion.domain.ImportacionRepositorio;

import java.time.Clock;
import java.util.UUID;

public class IniciarImportacion {

    private final ImportacionRepositorio importaciones;
    private final Clock reloj;

    public IniciarImportacion(ImportacionRepositorio importaciones, Clock reloj) {
        this.importaciones = importaciones;
        this.reloj = reloj;
    }

    public Importacion ejecutar(String nombreArchivo) {
        Importacion imp = Importacion.iniciar(UUID.randomUUID(), nombreArchivo, reloj.instant());
        importaciones.guardar(imp);
        return imp;
    }
}
