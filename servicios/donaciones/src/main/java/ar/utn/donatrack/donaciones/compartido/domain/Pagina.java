package ar.utn.donatrack.donaciones.compartido.domain;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> elementos, int numero, int tamanio, long total) {

    public <R> Pagina<R> mapear(Function<T, R> f) {
        return new Pagina<>(elementos.stream().map(f).toList(), numero, tamanio, total);
    }

    public int totalPaginas() {
        return tamanio == 0 ? 0 : (int) Math.ceil((double) total / tamanio);
    }
}
