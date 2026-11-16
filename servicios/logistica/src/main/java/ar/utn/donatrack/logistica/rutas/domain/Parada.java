package ar.utn.donatrack.logistica.rutas.domain;

import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;

import java.util.List;
import java.util.UUID;

/**
 * Parada de una ruta (una dirección de entidad). Embebe el resumen de sus entregas, incluido el
 * donante: así "¿qué rutas llevan donaciones de X?" se resuelve con una consulta, sin JOINs.
 */
public record Parada(int orden, UUID entidadId, Destino destino, List<EntregaEnRuta> entregas) {

    public record EntregaEnRuta(UUID entregaId, UUID donacionId, UUID donanteId, String descripcion) {
    }

    public Parada {
        entregas = List.copyOf(entregas);
    }

    public static Parada de(int orden, UUID entidadId, Destino destino, List<Entrega> entregas) {
        return new Parada(orden, entidadId, destino, entregas.stream()
                .map(e -> new EntregaEnRuta(e.id(), e.donacionId(), e.donanteId(),
                        e.detalle() == null ? null : e.detalle().descripcion()))
                .toList());
    }
}
