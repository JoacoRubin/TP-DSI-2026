package ar.utn.donatrack.logistica.entregas.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;

import java.util.List;
import java.util.UUID;

/** "Luego, la entidad deberá cargar fotos de la donación recibida": permite sumarlas después. */
public class AgregarFotos {

    private final EntregaRepositorio entregas;

    public AgregarFotos(EntregaRepositorio entregas) {
        this.entregas = entregas;
    }

    public Entrega ejecutar(UUID entregaId, List<String> fotos, Actor actor) {
        Entrega e = entregas.buscar(entregaId).orElseThrow(() -> new RecursoNoEncontradoException("Entrega", entregaId));
        actor.exigirEntidad(e.entidadId());
        if (e.estado() != EstadoEntrega.ENTREGADA) {
            throw new ReglaDeNegocioException("Sólo se pueden cargar fotos de entregas recibidas");
        }
        e.agregarFotos(fotos);
        entregas.guardar(e);
        return e;
    }
}
