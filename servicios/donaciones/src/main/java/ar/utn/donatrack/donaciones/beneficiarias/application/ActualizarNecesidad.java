package ar.utn.donatrack.donaciones.beneficiarias.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;

import java.util.UUID;

public class ActualizarNecesidad {

    private final NecesidadRepositorio necesidades;

    public ActualizarNecesidad(NecesidadRepositorio necesidades) {
        this.necesidades = necesidades;
    }

    public Necesidad ejecutar(UUID id, String descripcion, int cantidad, String unidad, Solicitante solicitante) {
        Necesidad n = necesidades.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Necesidad", id));
        solicitante.exigirAdminOEntidad(n.entidadId());
        n.actualizar(descripcion, cantidad, unidad);
        necesidades.guardar(n);
        return n;
    }
}
