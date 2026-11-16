package ar.utn.donatrack.logistica.entregas.application;

import ar.utn.donatrack.logistica.entregas.domain.DetalleDonacion;
import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;

import java.time.Clock;
import java.util.UUID;

/** Donaciones avisa que una donación fue asignada. Idempotente por donación. */
public class CrearEntrega {

    public record Comando(UUID donacionId, UUID donanteId, String donanteNombre, UUID entidadId, String entidadNombre,
                          String direccion, double latitud, double longitud, String descripcion, String subcategoria,
                          int cantidad, String unidad, double pesoKg, double volumenM3) {
    }

    private final EntregaRepositorio entregas;
    private final Clock reloj;

    public CrearEntrega(EntregaRepositorio entregas, Clock reloj) {
        this.entregas = entregas;
        this.reloj = reloj;
    }

    public Entrega ejecutar(Comando c) {
        return entregas.buscarPorDonacion(c.donacionId())
                .filter(e -> e.estado() != EstadoEntrega.CANCELADA)
                .orElseGet(() -> {
                    Entrega e = Entrega.nueva(UUID.randomUUID(), c.donacionId(), c.donanteId(), c.donanteNombre(),
                            c.entidadId(), new Destino(c.entidadNombre(), c.direccion(), c.latitud(), c.longitud()),
                            new DetalleDonacion(c.descripcion(), c.subcategoria(), c.cantidad(), c.unidad()),
                            c.pesoKg(), c.volumenM3(), reloj.instant());
                    entregas.guardar(e);
                    return e;
                });
    }
}
