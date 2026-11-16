package ar.utn.donatrack.incentivos.misiones.domain;

import ar.utn.donatrack.incentivos.actividad.domain.HistorialDonante;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Domain Service: evalúa la misión en curso; si se cumplió otorga la insignia, desbloquea la
 * siguiente y vuelve a evaluar (una sola donación puede completar varias misiones en cadena).
 */
public class MotorIncentivos {

    public record Resultado(List<InsigniaObtenida> insignias, List<CategoriaDonante> ascensos) {
        public boolean huboCambios() {
            return !insignias.isEmpty();
        }
    }

    public record Progreso(Mision mision, int actual, int restante, int porcentaje) {
    }

    private final CatalogoMisiones catalogo;

    public MotorIncentivos(CatalogoMisiones catalogo) {
        this.catalogo = catalogo;
    }

    public Resultado evaluar(PerfilDonante perfil, HistorialDonante historial, YearMonth mes, Instant ahora) {
        List<InsigniaObtenida> insignias = new ArrayList<>();
        List<CategoriaDonante> ascensos = new ArrayList<>();
        Optional<Mision> actual = perfil.misionActual(catalogo);
        while (actual.isPresent() && actual.get().tipo().progreso(historial, mes) >= actual.get().objetivo()) {
            Mision m = actual.get();
            insignias.add(InsigniaObtenida.nueva(perfil.donanteId(), m.id(), m.insignia(), m.icono(), m.categoria(), ahora));
            perfil.avanzar(catalogo).ifPresent(ascensos::add);
            actual = perfil.misionActual(catalogo);
        }
        return new Resultado(insignias, ascensos);
    }

    public Optional<Progreso> progresoActual(PerfilDonante perfil, HistorialDonante historial, YearMonth mes) {
        return perfil.misionActual(catalogo).map(m -> {
            int actual = Math.min(m.tipo().progreso(historial, mes), m.objetivo());
            return new Progreso(m, actual, m.objetivo() - actual, (int) Math.round(100.0 * actual / m.objetivo()));
        });
    }

    public CatalogoMisiones catalogo() {
        return catalogo;
    }
}
