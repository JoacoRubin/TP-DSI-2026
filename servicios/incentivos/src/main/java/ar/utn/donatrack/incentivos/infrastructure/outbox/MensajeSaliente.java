package ar.utn.donatrack.incentivos.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

/**
 * Mensaje hacia otro servicio que quedó registrado en la MISMA transacción que los cambios
 * locales que lo originan. El id autoincremental conserva el orden de inserción, que es el
 * orden en que se despacha.
 */
@Entity
@Table(name = "outbox_mensajes", indexes = @Index(name = "ix_outbox_estado_id", columnList = "estado, id"))
public class MensajeSaliente {

    public static final int MAX_INTENTOS = 20;
    /** Espera tras la primera falla; se duplica en cada falla siguiente hasta {@link #ESPERA_MAXIMA}. */
    static final Duration ESPERA_INICIAL = Duration.ofSeconds(3);
    /** Con este tope, los 20 intentos cubren algo más de una hora de destino caído. */
    static final Duration ESPERA_MAXIMA = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 40)
    private String tipo;
    /** Agrupa los mensajes que deben despacharse en orden (acá, la cola). */
    @Column(nullable = false, length = 64)
    private String clave;
    @Column(nullable = false, length = 8000)
    private String payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoMensaje estado;
    private int intentos;
    @Column(length = 1000)
    private String ultimoError;
    @Column(nullable = false)
    private Instant creadoEn;
    private Instant enviadoEn;
    /** Antes de este instante no se reintenta (null: se puede intentar ya). */
    private Instant proximoIntento;

    protected MensajeSaliente() {
    }

    static MensajeSaliente nuevo(String tipo, String clave, String payload, Instant ahora) {
        MensajeSaliente m = new MensajeSaliente();
        m.tipo = tipo;
        m.clave = clave;
        m.payload = payload;
        m.estado = EstadoMensaje.PENDIENTE;
        m.creadoEn = ahora;
        return m;
    }

    void marcarEnviado(Instant ahora) {
        intentos++;
        estado = EstadoMensaje.ENVIADO;
        enviadoEn = ahora;
        ultimoError = null;
        proximoIntento = null;
    }

    /**
     * Un rechazo de negocio no mejora reintentando: falla en el acto. Una falla transitoria
     * (destino caído) se reintenta con espera exponencial: 3 s, 6 s, 12 s... hasta 5 min.
     */
    void registrarFallo(String error, boolean definitivo, Instant ahora) {
        intentos++;
        ultimoError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
        if (definitivo || intentos >= MAX_INTENTOS) {
            estado = EstadoMensaje.FALLIDO;
            proximoIntento = null;
            return;
        }
        Duration espera = ESPERA_INICIAL.multipliedBy(1L << Math.min(intentos - 1, 20));
        proximoIntento = ahora.plus(espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera);
    }

    /** Si todavía está esperando su próximo reintento. */
    boolean enEspera(Instant ahora) {
        return proximoIntento != null && ahora.isBefore(proximoIntento);
    }

    /** Sólo para bandejas en memoria (tests); en JPA lo asigna la base. */
    void asignarId(long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public String tipo() { return tipo; }
    public String clave() { return clave; }
    public String payload() { return payload; }
    public EstadoMensaje estado() { return estado; }
    public int intentos() { return intentos; }
    public String ultimoError() { return ultimoError; }
    public Instant creadoEn() { return creadoEn; }
    public Instant enviadoEn() { return enviadoEn; }
    public Instant proximoIntento() { return proximoIntento; }
}
