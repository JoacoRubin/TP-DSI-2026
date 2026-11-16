package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.util.List;

/** La entrega no pudo concretarse: se registra la justificación y se avisa a todas las partes. */
public class RegistrarEntregaNoSatisfactoria {

    private final DonacionRepositorio donaciones;
    private final DonanteRepositorio donantes;
    private final EntidadRepositorio entidades;
    private final Notificador notificador;
    private final List<String> emailsAdministracion;

    public RegistrarEntregaNoSatisfactoria(DonacionRepositorio donaciones, DonanteRepositorio donantes,
                                           EntidadRepositorio entidades, Notificador notificador,
                                           List<String> emailsAdministracion) {
        this.donaciones = donaciones;
        this.donantes = donantes;
        this.entidades = entidades;
        this.notificador = notificador;
        this.emailsAdministracion = List.copyOf(emailsAdministracion);
    }

    public void ejecutar(EventosLogistica.EntregaNoSatisfactoria evento) {
        Donacion d = donaciones.buscar(evento.donacionId()).orElse(null);
        if (d == null || d.estado() != EstadoDonacion.EN_TRASLADO) {
            return;
        }
        d.registrarEntregaFallida(evento.motivo(), evento.informadaEn());
        donaciones.guardar(d);

        String detalle = "La entrega de " + d.descripcionCorta() + " no pudo concretarse. Motivo: " + evento.motivo()
                + ". La donación volvió al depósito y será replanificada.";
        entidades.buscar(d.entidadId()).ifPresent(e ->
                notificador.notificar(e.comoDestinatario(), "Entrega no realizada", detalle, "ENTREGA_FALLIDA"));
        donantes.buscar(d.donanteId()).ifPresent(dn ->
                notificador.notificar(dn.comoDestinatario(), "Novedades sobre tu donación", detalle, "ENTREGA_FALLIDA"));
        emailsAdministracion.forEach(email -> notificador.notificar(Destinatario.porEmail("Administración", email),
                "[Revisión requerida] Entrega fallida", detalle + " Donación " + d.id(), "ENTREGA_FALLIDA"));
    }
}
