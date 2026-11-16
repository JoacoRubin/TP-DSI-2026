package ar.utn.donatrack.logistica.rutas.domain;

import java.util.ArrayList;
import java.util.List;

/** Restricción del proveedor: como máximo 100 donaciones por ejecución del planificador. */
public final class Lotes {

    public static final int MAXIMO_PLANIFICADOR = 100;

    private Lotes() {
    }

    public static <T> List<List<T>> partir(List<T> elementos, int tamanio) {
        List<List<T>> lotes = new ArrayList<>();
        for (int i = 0; i < elementos.size(); i += tamanio) {
            lotes.add(List.copyOf(elementos.subList(i, Math.min(i + tamanio, elementos.size()))));
        }
        return lotes;
    }
}
