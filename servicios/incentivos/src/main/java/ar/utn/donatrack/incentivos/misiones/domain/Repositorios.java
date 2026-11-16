package ar.utn.donatrack.incentivos.misiones.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puertos de persistencia del contexto de misiones. */
public final class Repositorios {

    private Repositorios() {
    }

    public interface PerfilRepositorio {
        Optional<PerfilDonante> buscar(UUID donanteId);

        List<PerfilDonante> buscarTodos(List<UUID> ids);

        void guardar(PerfilDonante perfil);
    }

    public interface InsigniaRepositorio {
        List<InsigniaObtenida> deDonante(UUID donanteId);

        List<InsigniaObtenida> obtenidasEntre(Instant desde, Instant hasta);

        void guardarTodas(List<InsigniaObtenida> insignias);
    }
}
