package ar.utn.donatrack.donaciones.asignacion.domain;

import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.TipoNecesidad;
import ar.utn.donatrack.donaciones.donaciones.domain.Bien;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;

import java.util.Map;
import java.util.function.Supplier;

/**
 * S = w1·v1 + w2·v2 + w3·v3
 * <ul>
 *   <li>v1: similitud textual (Levenshtein) entre el bien y la descripción de la necesidad
 *       (se toma el bien más parecido de la donación)</li>
 *   <li>v2: cobertura de volumen. r = donado / pendiente; r ≤ 1 → r ; r > 1 → 1/r (penaliza desperdicio)</li>
 *   <li>v3: tipo de necesidad (extraordinaria 1 ; recurrente 0.5), ajustable</li>
 * </ul>
 */
public class CompatibilidadSemantica implements AlgoritmoMatchmaking {

    public static final String NOMBRE = "Compatibilidad semántica";
    public static final Map<TipoNecesidad, Double> VALORES_TIPO_PREDETERMINADOS =
            Map.of(TipoNecesidad.EXTRAORDINARIA, 1.0, TipoNecesidad.RECURRENTE, 0.5);

    private final Supplier<PesosSemanticos> pesos;
    private final Map<TipoNecesidad, Double> valoresTipo;

    public CompatibilidadSemantica(Supplier<PesosSemanticos> pesos) {
        this(pesos, VALORES_TIPO_PREDETERMINADOS);
    }

    public CompatibilidadSemantica(Supplier<PesosSemanticos> pesos, Map<TipoNecesidad, Double> valoresTipo) {
        this.pesos = pesos;
        this.valoresTipo = Map.copyOf(valoresTipo);
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public double puntuar(Donacion donacion, Necesidad necesidad, ContextoMatchmaking contexto) {
        PesosSemanticos w = pesos.get();
        double v1 = donacion.bienes().stream()
                .map(Bien::descripcion)
                .mapToDouble(nombre -> Levenshtein.similitud(nombre, necesidad.descripcion()))
                .max().orElse(0);
        double v2 = coberturaVolumen(donacion.cantidadTotal(), necesidad.cantidadPendiente(contexto.hoy()));
        double v3 = valoresTipo.getOrDefault(necesidad.tipo(), 0.0);
        return w.textual() * v1 + w.volumen() * v2 + w.tipo() * v3;
    }

    static double coberturaVolumen(int donado, int requerido) {
        if (requerido <= 0 || donado <= 0) {
            return 0;
        }
        double r = (double) donado / requerido;
        return r <= 1 ? r : 1 / r;
    }
}
