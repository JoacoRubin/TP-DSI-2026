package ar.utn.donatrack.donaciones.asignacion.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Top N de un algoritmo, ordenado de mayor a menor puntaje. */
public record Ranking(String algoritmo, List<Puntaje> puntajes) {

    public Ranking {
        puntajes = List.copyOf(puntajes);
    }

    public boolean contiene(UUID necesidadId) {
        return puntaje(necesidadId).isPresent();
    }

    public Optional<Puntaje> puntaje(UUID necesidadId) {
        return puntajes.stream().filter(p -> p.necesidadId().equals(necesidadId)).findFirst();
    }
}
