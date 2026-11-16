package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;

import java.util.UUID;

/** Los bienes no se editan (afectaría la trazabilidad); sólo datos descriptivos. */
public class ActualizarDonacion {

    private final DonacionRepositorio donaciones;

    public ActualizarDonacion(DonacionRepositorio donaciones) {
        this.donaciones = donaciones;
    }

    public Donacion ejecutar(UUID id, String descripcionGeneral) {
        Donacion d = donaciones.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Donación", id));
        d.actualizarDescripcion(descripcionGeneral);
        donaciones.guardar(d);
        return d;
    }
}
