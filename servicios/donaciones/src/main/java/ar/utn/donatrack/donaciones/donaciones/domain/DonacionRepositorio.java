package ar.utn.donatrack.donaciones.donaciones.domain;

import ar.utn.donatrack.donaciones.compartido.domain.Pagina;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DonacionRepositorio {

    Optional<Donacion> buscar(UUID id);

    List<Donacion> buscarTodas(Collection<UUID> ids);

    void guardar(Donacion donacion);

    void guardarTodas(Collection<Donacion> donaciones);

    void eliminar(UUID id);

    Pagina<Donacion> buscar(FiltroDonaciones filtro, int pagina, int tamanio);

    List<Donacion> enEstado(EstadoDonacion estado);

    /** Entregas exitosas por entidad desde una fecha (insumo del algoritmo de sub-atendidos). */
    Map<UUID, Integer> entregadasPorEntidadDesde(Instant desde);

    List<Donacion> entregadasDesde(Instant desde, int limite);
}
