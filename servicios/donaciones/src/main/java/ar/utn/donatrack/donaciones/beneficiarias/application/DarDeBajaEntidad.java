package ar.utn.donatrack.donaciones.beneficiarias.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;

import java.util.UUID;

/** Baja lógica de la entidad y de todas sus necesidades (dejan de participar del matchmaking). */
public class DarDeBajaEntidad {

    private final EntidadRepositorio entidades;
    private final NecesidadRepositorio necesidades;

    public DarDeBajaEntidad(EntidadRepositorio entidades, NecesidadRepositorio necesidades) {
        this.entidades = entidades;
        this.necesidades = necesidades;
    }

    public void ejecutar(UUID id) {
        EntidadBeneficiaria e = entidades.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Entidad", id));
        e.darDeBaja();
        entidades.guardar(e);
        necesidades.deEntidad(id).forEach(n -> {
            n.darDeBaja();
            necesidades.guardar(n);
        });
    }
}
