package ar.utn.donatrack.front.limites;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Aplica {@link PoliticaLimite} a cada solicitud. Se registra ANTES de Spring Security
 * (ver {@link ConfiguracionLimites}): un intento de fuerza bruta se corta antes de procesar la clave.
 * Clave del cliente: la IP de la conexión. Detrás de un proxy habría que usar X-Forwarded-For
 * SÓLO si el proxy es de confianza (si no, el atacante falsifica la IP y evade el límite).
 */
public class FiltroLimiteSolicitudes extends OncePerRequestFilter {

    private final LimitadorSolicitudes limitador;
    private final MeterRegistry metricas;

    public FiltroLimiteSolicitudes(LimitadorSolicitudes limitador, MeterRegistry metricas) {
        this.limitador = limitador;
        this.metricas = metricas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain cadena)
            throws ServletException, IOException {
        Optional<PoliticaLimite> politica = PoliticaLimite.para(req.getMethod(), req.getRequestURI());
        if (politica.isEmpty()) {
            cadena.doFilter(req, res);
            return;
        }
        LimitadorSolicitudes.Resultado r = limitador.consumir(politica.get(), req.getRemoteAddr());
        res.setHeader("X-RateLimit-Remaining", String.valueOf(r.restantes()));
        if (r.permitida()) {
            cadena.doFilter(req, res);
            return;
        }
        metricas.counter("donatrack.limite.rechazos", "politica", politica.get().name()).increment();
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setHeader("Retry-After", String.valueOf(r.esperarSegundos()));
        res.setContentType("text/plain;charset=UTF-8");
        res.getOutputStream().write(("Demasiadas solicitudes. Probá de nuevo en " + r.esperarSegundos() + " segundos.")
                .getBytes(StandardCharsets.UTF_8));
    }
}
