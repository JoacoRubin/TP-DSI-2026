package ar.utn.donatrack.donaciones.catalogo.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.catalogo.domain.Catalogo;
import ar.utn.donatrack.donaciones.catalogo.domain.CatalogoRepositorio;
import ar.utn.donatrack.donaciones.catalogo.domain.Categoria;
import ar.utn.donatrack.donaciones.catalogo.domain.Subcategoria;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categorias")
class CategoriaJpa {

    @Embeddable
    static class SubcategoriaJpa {
        private UUID subcategoriaId;
        private String nombre;

        protected SubcategoriaJpa() {
        }

        SubcategoriaJpa(UUID id, String nombre) {
            this.subcategoriaId = id;
            this.nombre = nombre;
        }
    }

    @Id
    private UUID id;
    private String nombre;
    private boolean perecedera;
    private boolean requiereEstadoUso;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subcategorias", joinColumns = @JoinColumn(name = "categoria_id"))
    private List<SubcategoriaJpa> subcategorias = new ArrayList<>();

    protected CategoriaJpa() {
    }

    static CategoriaJpa desde(Categoria c) {
        CategoriaJpa j = new CategoriaJpa();
        j.id = c.id();
        j.nombre = c.nombre();
        j.perecedera = c.perecedera();
        j.requiereEstadoUso = c.requiereEstadoUso();
        j.subcategorias = new ArrayList<>(c.subcategorias().stream().map(s -> new SubcategoriaJpa(s.id(), s.nombre())).toList());
        return j;
    }

    Categoria aDominio() {
        return new Categoria(id, nombre, perecedera, requiereEstadoUso, subcategorias.stream()
                .map(s -> new Subcategoria(s.subcategoriaId, s.nombre))
                .sorted(Comparator.comparing(Subcategoria::nombre)).toList());
    }
}

interface CategoriaJpaRepository extends JpaRepository<CategoriaJpa, UUID> {
}

/** El catálogo cambia muy poco: se cachea en memoria y se invalida al guardar. */
@Repository
@Transactional
class RepositorioCatalogoJpa implements CatalogoRepositorio {

    private final CategoriaJpaRepository jpa;
    private volatile Catalogo cache;

    RepositorioCatalogoJpa(CategoriaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Catalogo catalogo() {
        Catalogo c = cache;
        if (c == null) {
            c = new Catalogo(jpa.findAll().stream().map(CategoriaJpa::aDominio)
                    .sorted(Comparator.comparing(Categoria::nombre)).toList());
            cache = c;
        }
        return c;
    }

    @Override
    public void guardar(Categoria categoria) {
        jpa.save(CategoriaJpa.desde(categoria));
        cache = null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaVacio() {
        return jpa.count() == 0;
    }
}
