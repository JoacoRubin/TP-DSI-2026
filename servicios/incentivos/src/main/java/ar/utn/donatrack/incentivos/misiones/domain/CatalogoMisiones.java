package ar.utn.donatrack.incentivos.misiones.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Conjunto ordenado de misiones por categoría. */
public record CatalogoMisiones(List<Mision> misiones) {

    public CatalogoMisiones {
        misiones = misiones.stream()
                .sorted(Comparator.comparing(Mision::categoria).thenComparingInt(Mision::orden))
                .toList();
    }

    public List<Mision> de(CategoriaDonante categoria) {
        return misiones.stream().filter(m -> m.categoria() == categoria).toList();
    }

    public Optional<Mision> en(CategoriaDonante categoria, int indice) {
        List<Mision> lista = de(categoria);
        return indice >= 0 && indice < lista.size() ? Optional.of(lista.get(indice)) : Optional.empty();
    }

    public static CatalogoMisiones predeterminado() {
        return new CatalogoMisiones(List.of(
                new Mision("COL-1", CategoriaDonante.COLABORADOR, 1, "Primer paso", "Realizá tu primera donación",
                        TipoMision.COMPLETITUD, 1, "Primer paso", "🌱"),
                new Mision("COL-2", CategoriaDonante.COLABORADOR, 2, "Manos que llegan",
                        "Lográ que una entidad reciba tu donación", TipoMision.DONACIONES_EXITOSAS, 1, "Manos que llegan", "🤝"),
                new Mision("COL-3", CategoriaDonante.COLABORADOR, 3, "Constancia", "Doná 2 meses seguidos",
                        TipoMision.RACHA, 2, "Constancia", "🔥"),
                new Mision("SOS-1", CategoriaDonante.SOSTENEDOR, 1, "Versátil", "Doná bienes de 3 categorías distintas",
                        TipoMision.COMPLETITUD, 3, "Versátil", "🎨"),
                new Mision("SOS-2", CategoriaDonante.SOSTENEDOR, 2, "Hábil donador/a",
                        "Hacé una donación de más de 50 bienes en una sola carga", TipoMision.HABIL_DONADOR, 51,
                        "Hábil donador/a", "📦"),
                new Mision("SOS-3", CategoriaDonante.SOSTENEDOR, 3, "Impacto real", "Lográ 5 donaciones recibidas con éxito",
                        TipoMision.DONACIONES_EXITOSAS, 5, "Impacto real", "🎯"),
                new Mision("TRA-1", CategoriaDonante.TRANSFORMADOR, 1, "Corazón constante", "Doná 6 meses seguidos",
                        TipoMision.RACHA, 6, "Corazón constante", "❤️"),
                new Mision("TRA-2", CategoriaDonante.TRANSFORMADOR, 2, "Todo terreno",
                        "Doná bienes de 5 categorías distintas", TipoMision.COMPLETITUD, 5, "Todo terreno", "🌍"),
                new Mision("TRA-3", CategoriaDonante.TRANSFORMADOR, 3, "Agente de cambio",
                        "Lográ 20 donaciones recibidas con éxito", TipoMision.DONACIONES_EXITOSAS, 20, "Agente de cambio", "🏆")));
    }
}
