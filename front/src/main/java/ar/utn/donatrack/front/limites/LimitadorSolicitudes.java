package ar.utn.donatrack.front.limites;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;

import java.time.Duration;

/**
 * Token bucket por (política, cliente). Los buckets viven en una cache acotada: un atacante que
 * rota IPs no puede agotar la memoria. En memoria del proceso: con varias instancias del front
 * habría que moverlo a Redis (bucket4j-redis) o al API Gateway.
 */
public class LimitadorSolicitudes {

    public record Resultado(boolean permitida, long restantes, long esperarSegundos) {
    }

    private final TimeMeter reloj;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(15))
            .build();

    public LimitadorSolicitudes() {
        this(TimeMeter.SYSTEM_MILLISECONDS);
    }

    LimitadorSolicitudes(TimeMeter reloj) {
        this.reloj = reloj;
    }

    public Resultado consumir(PoliticaLimite politica, String cliente) {
        Bucket bucket = buckets.get(politica.name() + "|" + cliente, k -> nuevo(politica));
        ConsumptionProbe p = bucket.tryConsumeAndReturnRemaining(1);
        long esperar = p.isConsumed() ? 0 : Math.max(1, Duration.ofNanos(p.getNanosToWaitForRefill()).toSeconds() + 1);
        return new Resultado(p.isConsumed(), p.getRemainingTokens(), esperar);
    }

    private Bucket nuevo(PoliticaLimite politica) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(politica.capacidad())
                        .refillGreedy(politica.capacidad(), politica.periodo()).build())
                .withCustomTimePrecision(reloj)
                .build();
    }
}
