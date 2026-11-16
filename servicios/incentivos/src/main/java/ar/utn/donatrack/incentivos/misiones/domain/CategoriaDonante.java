package ar.utn.donatrack.incentivos.misiones.domain;

import java.util.Optional;

/** Categorías visibles públicamente junto al nombre del donante. */
public enum CategoriaDonante {
    COLABORADOR("Colaborador/a"),
    SOSTENEDOR("Sostenedor/a"),
    TRANSFORMADOR("Transformador/a");

    private final String etiqueta;

    CategoriaDonante(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public Optional<CategoriaDonante> siguiente() {
        return ordinal() + 1 < values().length ? Optional.of(values()[ordinal() + 1]) : Optional.empty();
    }

    public String etiqueta() {
        return etiqueta;
    }
}
