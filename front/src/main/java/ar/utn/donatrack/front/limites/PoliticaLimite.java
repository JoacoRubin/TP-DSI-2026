package ar.utn.donatrack.front.limites;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Cupos de tráfico por cliente (IP) en el borde del sistema. El front es el BFF: todo el
 * tráfico del navegador entra por acá, así que es el punto natural para limitarlo.
 */
public enum PoliticaLimite {

    /** Login, registro y activación: frena fuerza bruta de contraseñas y altas masivas. */
    AUTENTICACION(10, Duration.ofMinutes(1)),
    /** Reportes GPS del chofer: 1 por segundo alcanza para seguimiento en tiempo real. */
    GPS(60, Duration.ofMinutes(1)),
    /** Navegación general: protege de scraping y sobrecarga. */
    GENERAL(300, Duration.ofMinutes(1));

    private static final Set<String> SIN_LIMITE = Set.of("/css/", "/js/", "/img/", "/webjars/", "/actuator/", "/favicon");

    private final int capacidad;
    private final Duration periodo;

    PoliticaLimite(int capacidad, Duration periodo) {
        this.capacidad = capacidad;
        this.periodo = periodo;
    }

    public int capacidad() { return capacidad; }
    public Duration periodo() { return periodo; }

    /** Vacío = no se limita (recursos estáticos y sondas de salud/métricas). */
    public static Optional<PoliticaLimite> para(String metodo, String uri) {
        if (SIN_LIMITE.stream().anyMatch(uri::startsWith)) {
            return Optional.empty();
        }
        boolean post = "POST".equalsIgnoreCase(metodo);
        if (post && (uri.equals("/login") || uri.startsWith("/registro") || uri.equals("/activar"))) {
            return Optional.of(AUTENTICACION);
        }
        if (post && uri.startsWith("/front-api/chofer/rutas/") && uri.endsWith("/posiciones")) {
            return Optional.of(GPS);
        }
        return Optional.of(GENERAL);
    }
}
