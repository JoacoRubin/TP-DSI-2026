package ar.utn.donatrack.logistica.entregas.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntregaRepositorio {

    Optional<Entrega> buscar(UUID id);

    Optional<Entrega> buscarPorDonacion(UUID donacionId);

    List<Entrega> buscarTodas(Collection<UUID> ids);

    List<Entrega> enEstado(EstadoEntrega estado);

    List<Entrega> deRuta(UUID rutaId);

    List<Entrega> filtrar(EstadoEntrega estado, UUID entidadId, UUID donanteId);

    List<Entrega> entregadasDesde(Instant desde, int limite);

    void guardar(Entrega entrega);

    void guardarTodas(Collection<Entrega> entregas);
}
