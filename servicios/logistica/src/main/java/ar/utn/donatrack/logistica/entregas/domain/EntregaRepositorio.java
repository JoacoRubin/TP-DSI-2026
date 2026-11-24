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

    /** Cantidad de entregas realizadas desde {@code desde} y de entidades distintas que las recibieron. */
    ResumenEntregas resumenEntregadasDesde(Instant desde);

    /** Entregas de esas donaciones (puede haber más de una por donación), de la más nueva a la más vieja. */
    List<Entrega> deDonaciones(Collection<UUID> donacionIds);

    /** Consulta paginada con filtros dinámicos, de la más nueva a la más vieja. */
    Pagina<Entrega> buscar(FiltroEntregas filtro, int numero, int tamanio);

    void guardar(Entrega entrega);

    /**
     * Alta atómica e idempotente por donación: si ya hay una entrega NO cancelada para la misma
     * donación (aunque la haya creado otra solicitud concurrente), devuelve esa en lugar de duplicar.
     */
    Entrega registrarSiNoHayActiva(Entrega nueva);

    void guardarTodas(Collection<Entrega> entregas);
}
