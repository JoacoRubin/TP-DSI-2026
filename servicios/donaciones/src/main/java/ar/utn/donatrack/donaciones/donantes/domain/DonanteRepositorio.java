package ar.utn.donatrack.donaciones.donantes.domain;

import ar.utn.donatrack.donaciones.compartido.domain.Pagina;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DonanteRepositorio {

    Optional<Donante> buscar(UUID id);

    Optional<Donante> buscarPorEmail(String email);

    List<Donante> buscarPorEmails(Collection<String> emails);

    boolean existeEmail(String email);

    void guardar(Donante donante);

    void guardarTodos(Collection<? extends Donante> donantes);

    Pagina<Donante> listar(String texto, int pagina, int tamanio);

    /** Donantes activos cuya última interacción es anterior al límite. */
    List<Donante> sinInteraccionDesde(Instant limite);
}
