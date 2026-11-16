package ar.utn.donatrack.donaciones.beneficiarias.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;

import java.util.List;
import java.util.UUID;

public class ActualizarEntidad {

    private final EntidadRepositorio entidades;

    public ActualizarEntidad(EntidadRepositorio entidades) {
        this.entidades = entidades;
    }

    public EntidadBeneficiaria ejecutar(UUID id, String razonSocial, Direccion direccion, String telefono,
                                       List<String> emails, Solicitante solicitante) {
        solicitante.exigirAdminOEntidad(id);
        EntidadBeneficiaria e = entidades.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Entidad", id));
        e.actualizar(razonSocial, direccion, telefono, emails);
        entidades.guardar(e);
        return e;
    }
}
