package ar.utn.donatrack.donaciones.donantes.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.util.UUID;

/** Baja lógica: el historial de donaciones debe seguir siendo trazable. */
public class DarDeBajaDonante {

    private final DonanteRepositorio donantes;

    public DarDeBajaDonante(DonanteRepositorio donantes) {
        this.donantes = donantes;
    }

    public void ejecutar(UUID donanteId) {
        Donante d = donantes.buscar(donanteId).orElseThrow(() -> new RecursoNoEncontradoException("Donante", donanteId));
        d.darDeBaja();
        donantes.guardar(d);
    }
}
