package ar.utn.donatrack.donaciones.beneficiarias.application;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.AltaDeCuenta;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class RegistrarEntidad {

    public record Comando(String razonSocial, Direccion direccion, String telefono, List<String> emails, String clave) {
    }

    private final EntidadRepositorio entidades;
    private final AltaDeCuenta altaDeCuenta;
    private final Clock reloj;

    public RegistrarEntidad(EntidadRepositorio entidades, AltaDeCuenta altaDeCuenta, Clock reloj) {
        this.entidades = entidades;
        this.altaDeCuenta = altaDeCuenta;
        this.reloj = reloj;
    }

    public EntidadBeneficiaria ejecutar(Comando c) {
        EntidadBeneficiaria entidad = EntidadBeneficiaria.registrar(UUID.randomUUID(), c.razonSocial(), c.direccion(),
                c.telefono(), c.emails(), reloj.instant());
        if (entidades.existeEmail(entidad.emailPrincipal())) {
            throw new ReglaDeNegocioException("Ya existe una entidad con el email " + entidad.emailPrincipal());
        }
        entidades.guardar(entidad);
        altaDeCuenta.crear(entidad.comoDestinatario(), "BENEFICIARIA", null, entidad.id().toString(), c.clave());
        return entidad;
    }
}
