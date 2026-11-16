package ar.utn.donatrack.donaciones.donantes.application;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.application.AltaDeCuenta;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaJuridica;
import ar.utn.donatrack.donaciones.donantes.domain.Representante;
import ar.utn.donatrack.donaciones.donantes.domain.TipoPersonaJuridica;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class RegistrarPersonaJuridica {

    public record Comando(String razonSocial, TipoPersonaJuridica tipo, String rubro, Documento cuit,
                          Direccion direccion, MediosContacto contacto, List<Representante> representantes,
                          String clave) {
    }

    private final DonanteRepositorio donantes;
    private final AltaDeCuenta altaDeCuenta;
    private final Clock reloj;

    public RegistrarPersonaJuridica(DonanteRepositorio donantes, AltaDeCuenta altaDeCuenta, Clock reloj) {
        this.donantes = donantes;
        this.altaDeCuenta = altaDeCuenta;
        this.reloj = reloj;
    }

    public PersonaJuridica ejecutar(Comando c) {
        if (donantes.existeEmail(c.contacto().email())) {
            throw new ReglaDeNegocioException("Ya existe una persona donante con el email " + c.contacto().email());
        }
        PersonaJuridica org = PersonaJuridica.registrar(UUID.randomUUID(), c.razonSocial(), c.tipo(), c.rubro(),
                c.cuit(), c.direccion(), c.contacto(), c.representantes(), reloj.instant());
        donantes.guardar(org);
        altaDeCuenta.crear(org.comoDestinatario(), "DONANTE", "JURIDICA", org.id().toString(), c.clave());
        return org;
    }
}
