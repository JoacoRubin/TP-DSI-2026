package ar.utn.donatrack.notificaciones.envios.domain;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Notificacion {

    public static final int MAX_INTENTOS = 5;

    private final UUID id;
    private final Destinatario destinatario;
    private final String asunto;
    private final String mensaje;
    private final String tipo;
    private final String origen;
    private final MedioNotificacion medioSolicitado;
    private MedioNotificacion medioUtilizado;
    private EstadoNotificacion estado;
    private int intentos;
    private String ultimoError;
    private final Instant creadaEn;
    private Instant enviadaEn;
    private boolean leida;

    public Notificacion(UUID id, Destinatario destinatario, String asunto, String mensaje, String tipo, String origen,
                        MedioNotificacion medioSolicitado, MedioNotificacion medioUtilizado, EstadoNotificacion estado,
                        int intentos, String ultimoError, Instant creadaEn, Instant enviadaEn, boolean leida) {
        this.id = Objects.requireNonNull(id);
        this.destinatario = Objects.requireNonNull(destinatario);
        this.asunto = asunto;
        this.mensaje = Objects.requireNonNull(mensaje);
        this.tipo = tipo;
        this.origen = origen;
        this.medioSolicitado = medioSolicitado;
        this.medioUtilizado = medioUtilizado;
        this.estado = estado;
        this.intentos = intentos;
        this.ultimoError = ultimoError;
        this.creadaEn = creadaEn;
        this.enviadaEn = enviadaEn;
        this.leida = leida;
    }

    public static Notificacion nueva(UUID id, Destinatario d, String asunto, String mensaje, String tipo, String origen,
                                     MedioNotificacion medio, Instant ahora) {
        return new Notificacion(id, d, asunto, mensaje, tipo, origen, medio, null, EstadoNotificacion.PENDIENTE,
                0, null, ahora, null, false);
    }

    /** Intenta el envío por el canal elegido. Nunca propaga la excepción: registra el fallo. */
    public void intentarEnvio(SelectorCanal selector, Instant ahora) {
        if (estado == EstadoNotificacion.ENVIADA) {
            return;
        }
        CanalNotificacion canal = selector.elegir(destinatario, medioSolicitado);
        intentos++;
        medioUtilizado = canal.medio();
        try {
            canal.enviar(destinatario, asunto, mensaje);
            estado = EstadoNotificacion.ENVIADA;
            enviadaEn = ahora;
            ultimoError = null;
        } catch (RuntimeException e) {
            estado = EstadoNotificacion.FALLIDA;
            ultimoError = e.getMessage();
        }
    }

    public boolean esReintentable() {
        return estado == EstadoNotificacion.FALLIDA && intentos < MAX_INTENTOS;
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public UUID id() { return id; }
    public Destinatario destinatario() { return destinatario; }
    public String asunto() { return asunto; }
    public String mensaje() { return mensaje; }
    public String tipo() { return tipo; }
    public String origen() { return origen; }
    public MedioNotificacion medioSolicitado() { return medioSolicitado; }
    public MedioNotificacion medioUtilizado() { return medioUtilizado; }
    public EstadoNotificacion estado() { return estado; }
    public int intentos() { return intentos; }
    public String ultimoError() { return ultimoError; }
    public Instant creadaEn() { return creadaEn; }
    public Instant enviadaEn() { return enviadaEn; }
    public boolean leida() { return leida; }
}
