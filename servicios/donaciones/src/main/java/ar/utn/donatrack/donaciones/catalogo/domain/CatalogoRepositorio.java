package ar.utn.donatrack.donaciones.catalogo.domain;

public interface CatalogoRepositorio {

    Catalogo catalogo();

    void guardar(Categoria categoria);

    boolean estaVacio();
}
