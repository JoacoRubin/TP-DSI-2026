package ar.utn.donatrack.donaciones.donantes.application;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.application.AltaDeCuenta;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;

import java.time.Clock;
import java.util.UUID;

/**
 * Alta de persona humana donante. Dos caminos: la administración en el depósito (sin clave →
 * correo de bienvenida con enlace de activación) o autorregistro desde la web (con clave).
 */
public class RegistrarPersonaHumana {

    public record Comando(String nombre, String apellido, Integer edad, Documento documento, Genero genero,
                          Direccion direccion, MediosContacto contacto, String clave) {
    }

    private final DonanteRepositorio donantes;
    private final AltaDeCuenta altaDeCuenta;
    private final Clock reloj;

    public RegistrarPersonaHumana(DonanteRepositorio donantes, AltaDeCuenta altaDeCuenta, Clock reloj) {
        this.donantes = donantes;
        this.altaDeCuenta = altaDeCuenta;
        this.reloj = reloj;
    }

    public PersonaHumana ejecutar(Comando c) {
        if (donantes.existeEmail(c.contacto().email())) {
            throw new ReglaDeNegocioException("Ya existe una persona donante con el email " + c.contacto().email());
        }
        PersonaHumana persona = PersonaHumana.registrar(UUID.randomUUID(), c.nombre(), c.apellido(), c.edad(),
                c.documento(), c.genero(), c.direccion(), c.contacto(), reloj.instant());
        donantes.guardar(persona);
        altaDeCuenta.crear(persona.comoDestinatario(), "DONANTE", "HUMANA", persona.id().toString(), c.clave());
        return persona;
    }
}
