package ar.utn.donatrack.notificaciones.envios.infrastructure.web;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
import ar.utn.donatrack.notificaciones.envios.application.ConsultarNotificaciones;
import ar.utn.donatrack.notificaciones.envios.application.EnviarNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.Notificacion;
import ar.utn.donatrack.soporte.seguridad.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
class NotificacionesController {

    private final EnviarNotificacion enviar;
    private final ConsultarNotificaciones consultar;

    NotificacionesController(EnviarNotificacion enviar, ConsultarNotificaciones consultar) {
        this.enviar = enviar;
        this.consultar = consultar;
    }

    /** Componente notificador (Entrega 1): destinatario + mensaje + medio → envío. */
    record EnvioRequest(@NotBlank String nombre, @NotBlank @Email String email, String telefono, String whatsapp,
                        MedioNotificacion medio, String asunto, @NotBlank String mensaje) {
    }

    record NotificacionVista(UUID id, String asunto, String mensaje, String tipo, String medio, String estado,
                             Instant creadaEn, boolean leida) {
        static NotificacionVista de(Notificacion n) {
            return new NotificacionVista(n.id(), n.asunto(), n.mensaje(), n.tipo(),
                    n.medioUtilizado() == null ? null : n.medioUtilizado().name(), n.estado().name(), n.creadaEn(), n.leida());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    NotificacionVista enviar(@Valid @RequestBody EnvioRequest req) {
        Destinatario d = new Destinatario(req.nombre(), req.email(), req.telefono(), req.whatsapp(), req.medio());
        return NotificacionVista.de(enviar.ejecutar(new SolicitudNotificacion(UUID.randomUUID(), d, req.asunto(),
                req.mensaje(), "MANUAL", "api", req.medio(), Instant.now())));
    }

    @GetMapping("/mias")
    List<NotificacionVista> mias(Authentication auth) {
        return consultar.de(Claims.usuario(auth)).stream().map(NotificacionVista::de).toList();
    }

    @PatchMapping("/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void marcarLeida(@PathVariable UUID id, Authentication auth) {
        consultar.marcarLeida(id, Claims.usuario(auth));
    }
}
