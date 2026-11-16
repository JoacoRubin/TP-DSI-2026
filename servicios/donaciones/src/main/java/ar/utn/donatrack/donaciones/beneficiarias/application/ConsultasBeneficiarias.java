package ar.utn.donatrack.donaciones.beneficiarias.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;

import java.util.List;
import java.util.UUID;

public class ConsultasBeneficiarias {

    private final EntidadRepositorio entidades;
    private final NecesidadRepositorio necesidades;

    public ConsultasBeneficiarias(EntidadRepositorio entidades, NecesidadRepositorio necesidades) {
        this.entidades = entidades;
        this.necesidades = necesidades;
    }

    public List<EntidadBeneficiaria> listar(String texto) {
        return entidades.listarActivas(texto);
    }

    public EntidadBeneficiaria detalle(UUID id) {
        return entidades.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Entidad", id));
    }

    public List<Necesidad> necesidadesDe(UUID entidadId, boolean incluirInactivas) {
        return necesidades.deEntidad(entidadId).stream().filter(n -> incluirInactivas || n.activa()).toList();
    }

    public Necesidad necesidad(UUID id) {
        return necesidades.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Necesidad", id));
    }
}
