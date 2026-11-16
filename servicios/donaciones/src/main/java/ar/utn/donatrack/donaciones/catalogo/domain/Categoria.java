package ar.utn.donatrack.donaciones.catalogo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @param perecedera        sus bienes exigen fecha de vencimiento (ej. Alimentos)
 * @param requiereEstadoUso sus bienes exigen indicar nuevo/usado (ej. Mobiliario, Vestimenta)
 */
public record Categoria(UUID id, String nombre, boolean perecedera, boolean requiereEstadoUso,
                        List<Subcategoria> subcategorias) {

    public Categoria {
        subcategorias = List.copyOf(subcategorias);
    }

    public Optional<Subcategoria> subcategoria(UUID subcategoriaId) {
        return subcategorias.stream().filter(s -> s.id().equals(subcategoriaId)).findFirst();
    }
}
