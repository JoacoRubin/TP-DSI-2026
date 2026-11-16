package ar.utn.donatrack.logistica.flota.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.flota.domain.CamionRepositorio;

import java.util.List;
import java.util.UUID;

/**
 * ABM de camiones. Agrupado en una clase porque son operaciones CRUD triviales sin
 * orquestación: separarlas en 4 casos de uso no agregaría claridad (YAGNI).
 */
public class GestionarFlota {

    private final CamionRepositorio camiones;

    public GestionarFlota(CamionRepositorio camiones) {
        this.camiones = camiones;
    }

    public Camion registrar(String patente, double volumenM3, double alturaM, double cargaKg) {
        String normalizada = Camion.normalizarPatente(patente);
        if (camiones.buscarPorPatente(normalizada).isPresent()) {
            throw new ReglaDeNegocioException("Ya existe un camión con patente " + normalizada);
        }
        Camion c = Camion.registrar(UUID.randomUUID(), normalizada, volumenM3, alturaM, cargaKg);
        camiones.guardar(c);
        return c;
    }

    public Camion actualizar(UUID id, double volumenM3, double alturaM, double cargaKg, boolean activo) {
        Camion c = buscar(id);
        c.actualizar(volumenM3, alturaM, cargaKg);
        if (activo) {
            c.reactivar();
        } else {
            c.darDeBaja();
        }
        camiones.guardar(c);
        return c;
    }

    public void darDeBaja(UUID id) {
        Camion c = buscar(id);
        c.darDeBaja();
        camiones.guardar(c);
    }

    public List<Camion> listar() {
        return camiones.todos();
    }

    public Camion buscar(UUID id) {
        return camiones.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Camión", id));
    }
}
