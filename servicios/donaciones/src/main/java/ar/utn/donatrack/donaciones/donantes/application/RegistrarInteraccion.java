package ar.utn.donatrack.donaciones.donantes.application;

import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.time.Clock;
import java.util.UUID;

/** Se invoca cuando la persona donante usa la plataforma (ej. inicia sesión). */
public class RegistrarInteraccion {

    private final DonanteRepositorio donantes;
    private final Clock reloj;

    public RegistrarInteraccion(DonanteRepositorio donantes, Clock reloj) {
        this.donantes = donantes;
        this.reloj = reloj;
    }

    public void ejecutar(UUID donanteId) {
        donantes.buscar(donanteId).ifPresent(d -> {
            d.registrarInteraccion(reloj.instant());
            donantes.guardar(d);
        });
    }
}
