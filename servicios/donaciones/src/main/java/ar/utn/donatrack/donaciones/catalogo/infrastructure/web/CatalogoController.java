package ar.utn.donatrack.donaciones.catalogo.infrastructure.web;

import ar.utn.donatrack.donaciones.catalogo.domain.CatalogoRepositorio;
import ar.utn.donatrack.donaciones.catalogo.domain.Categoria;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class CatalogoController {

    private final CatalogoRepositorio catalogo;

    CatalogoController(CatalogoRepositorio catalogo) {
        this.catalogo = catalogo;
    }

    /** Requerimiento de implementación de la Entrega 1. */
    @GetMapping(value = "/api/saludo", produces = "text/plain;charset=UTF-8")
    String saludo() {
        return "Hola desde el servicio de Donaciones.";
    }

    @GetMapping("/api/categorias")
    List<Categoria> categorias() {
        return catalogo.catalogo().categorias();
    }
}
