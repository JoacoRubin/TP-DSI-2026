package ar.utn.donatrack.donaciones.beneficiarias.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;

import java.util.UUID;

public class DarDeBajaNecesidad {

    private final NecesidadRepositorio necesidades;

    public DarDeBajaNecesidad(NecesidadRepositorio necesidades) {
        this.necesidades = necesidades;
    }

    public void ejecutar(UUID id, Solicitante solicitante) {
        Necesidad n = necesidades.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Necesidad", id));
        solicitante.exigirAdminOEntidad(n.entidadId());
        n.darDeBaja();
        necesidades.guardar(n);
    }
}
