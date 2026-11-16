package ar.utn.donatrack.donaciones.catalogo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vista consultable del árbol categoría → subcategorías. */
public record Catalogo(List<Categoria> categorias) {

    public Catalogo {
        categorias = List.copyOf(categorias);
    }

    public Optional<Categoria> categoriaDe(UUID subcategoriaId) {
        return categorias.stream().filter(c -> c.subcategoria(subcategoriaId).isPresent()).findFirst();
    }

    public Optional<Subcategoria> subcategoria(UUID subcategoriaId) {
        return categorias.stream().flatMap(c -> c.subcategoria(subcategoriaId).stream()).findFirst();
    }
}
