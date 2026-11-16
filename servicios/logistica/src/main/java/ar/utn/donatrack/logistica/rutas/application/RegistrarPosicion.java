package ar.utn.donatrack.logistica.rutas.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.rutas.domain.Posicion;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import ar.utn.donatrack.logistica.rutas.domain.RutaRepositorio;

import java.time.Clock;
import java.util.UUID;

/**
 * Contrato de integración con la APP MÓVIL del chofer (alternativa elegida): la app reporta
 * su geolocalización mientras la ruta está activa. La plataforma recibe, valida y procesa.
 */
public class RegistrarPosicion {

    private final RutaRepositorio rutas;
    private final Clock reloj;

    public RegistrarPosicion(RutaRepositorio rutas, Clock reloj) {
        this.rutas = rutas;
        this.reloj = reloj;
    }

    public boolean ejecutar(UUID rutaId, Posicion posicion, Actor actor) {
        Ruta ruta = rutas.buscar(rutaId).orElseThrow(() -> new RecursoNoEncontradoException("Ruta", rutaId));
        actor.exigirChoferDe(ruta.patente());
        boolean aceptada = ruta.registrarPosicion(posicion, reloj.instant());
        if (aceptada) {
            rutas.guardarPosicion(ruta);
        }
        return aceptada;
    }
}
