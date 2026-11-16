package ar.utn.donatrack.donaciones.donantes.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;

import java.util.UUID;

public class ActualizarContactoDonante {

    private final DonanteRepositorio donantes;

    public ActualizarContactoDonante(DonanteRepositorio donantes) {
        this.donantes = donantes;
    }

    public Donante ejecutar(UUID donanteId, MediosContacto contacto, Direccion direccion, Solicitante solicitante) {
        solicitante.exigirAdminODonante(donanteId);
        Donante donante = donantes.buscar(donanteId).orElseThrow(() -> new RecursoNoEncontradoException("Donante", donanteId));
        if (!donante.email().equals(contacto.email())) {
            // El email es la identidad de la cuenta: cambiarlo requiere un flujo de verificación aparte.
            throw new ReglaDeNegocioException("El correo electrónico no puede modificarse desde aquí");
        }
        donante.actualizarContacto(contacto);
        if (direccion != null) {
            donante.actualizarDireccion(direccion);
        }
        donantes.guardar(donante);
        return donante;
    }
}
