package ar.utn.donatrack.logistica.entregas.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Criterios opcionales y combinables (AND) para consultar entregas. Un campo null / vacío no
 * filtra. {@code texto} busca en la descripción de la donación y en el nombre de la entidad.
 */
public record FiltroEntregas(Set<EstadoEntrega> estados, UUID entidadId, UUID donanteId, String patente,
                             Instant desde, Instant hasta, String texto) {

    public FiltroEntregas {
        estados = estados == null ? Set.of() : Set.copyOf(estados);
        patente = patente == null || patente.isBlank() ? null : patente.replaceAll("[\\s-]", "").toUpperCase();
        texto = texto == null || texto.isBlank() ? null : texto.trim();
    }

    public static FiltroEntregas sinFiltros() {
        return new FiltroEntregas(null, null, null, null, null, null, null);
    }

    public static FiltroEntregas conTexto(String texto) {
        return new FiltroEntregas(null, null, null, null, null, null, texto);
    }
}
