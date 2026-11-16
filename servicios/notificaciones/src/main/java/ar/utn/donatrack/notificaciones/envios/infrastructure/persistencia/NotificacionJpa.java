package ar.utn.donatrack.notificaciones.envios.infrastructure.persistencia;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.Notificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificaciones", indexes = {
        @Index(name = "ix_notif_email", columnList = "destinatarioEmail,creadaEn"),
        @Index(name = "ix_notif_estado", columnList = "estado")})
public class NotificacionJpa {

    @Id
    private UUID id;
    private String destinatarioNombre;
    @Column(nullable = false)
    private String destinatarioEmail;
    private String destinatarioTelefono;
    private String destinatarioWhatsapp;
    @Enumerated(EnumType.STRING)
    private MedioNotificacion medioPreferido;
    private String asunto;
    @Column(length = 4000, nullable = false)
    private String mensaje;
    private String tipo;
    private String origen;
    @Enumerated(EnumType.STRING)
    private MedioNotificacion medioSolicitado;
    @Enumerated(EnumType.STRING)
    private MedioNotificacion medioUtilizado;
    @Enumerated(EnumType.STRING)
    private EstadoNotificacion estado;
    private int intentos;
    @Column(length = 1000)
    private String ultimoError;
    private Instant creadaEn;
    private Instant enviadaEn;
    private boolean leida;

    protected NotificacionJpa() {
    }

    static NotificacionJpa desde(Notificacion n) {
        NotificacionJpa j = new NotificacionJpa();
        j.id = n.id();
        Destinatario d = n.destinatario();
        j.destinatarioNombre = d.nombre();
        j.destinatarioEmail = d.email().toLowerCase();
        j.destinatarioTelefono = d.telefono();
        j.destinatarioWhatsapp = d.whatsapp();
        j.medioPreferido = d.medioPreferido();
        j.asunto = n.asunto();
        j.mensaje = n.mensaje();
        j.tipo = n.tipo();
        j.origen = n.origen();
        j.medioSolicitado = n.medioSolicitado();
        j.medioUtilizado = n.medioUtilizado();
        j.estado = n.estado();
        j.intentos = n.intentos();
        j.ultimoError = n.ultimoError() == null ? null : n.ultimoError().substring(0, Math.min(1000, n.ultimoError().length()));
        j.creadaEn = n.creadaEn();
        j.enviadaEn = n.enviadaEn();
        j.leida = n.leida();
        return j;
    }

    Notificacion aDominio() {
        return new Notificacion(id, new Destinatario(destinatarioNombre, destinatarioEmail, destinatarioTelefono,
                destinatarioWhatsapp, medioPreferido), asunto, mensaje, tipo, origen, medioSolicitado, medioUtilizado,
                estado, intentos, ultimoError, creadaEn, enviadaEn, leida);
    }
}
