package ar.utn.donatrack.logistica.entregas.domain;

import java.util.List;

/** Una porción de resultados con el total REAL de coincidencias (no el tamaño de la porción). */
public record Pagina<T>(List<T> elementos, long total, int numero, int tamanio) {

    public Pagina {
        elementos = List.copyOf(elementos);
    }
}
