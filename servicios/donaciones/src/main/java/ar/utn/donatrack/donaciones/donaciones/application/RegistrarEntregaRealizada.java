package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.compartido.application.PublicadorEventos;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * La entidad confirmó la recepción: la donación queda "Entregada", la necesidad suma lo
 * recibido, se notifica con comprobante (fecha, hora, camión) y se informa a Incentivos.
 */
public class RegistrarEntregaRealizada {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final DonacionRepositorio donaciones;
    private final NecesidadRepositorio necesidades;
    private final DonanteRepositorio donantes;
    private final EntidadRepositorio entidades;
    private final Notificador notificador;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public RegistrarEntregaRealizada(DonacionRepositorio donaciones, NecesidadRepositorio necesidades,
                                     DonanteRepositorio donantes, EntidadRepositorio entidades,
                                     Notificador notificador, PublicadorEventos eventos, Clock reloj) {
        this.donaciones = donaciones;
        this.necesidades = necesidades;
        this.donantes = donantes;
        this.entidades = entidades;
        this.notificador = notificador;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public void ejecutar(EventosLogistica.EntregaRealizada evento) {
        Donacion d = donaciones.buscar(evento.donacionId()).orElse(null);
        if (d == null || d.estado() != EstadoDonacion.EN_TRASLADO) {
            return; // idempotencia: evento repetido o fuera de orden
        }
        d.confirmarEntrega(evento.patente(), evento.entregadaEn());
        donaciones.guardar(d);

        ZonedDateTime momento = evento.entregadaEn().atZone(reloj.getZone());
        necesidades.buscar(d.necesidadId()).ifPresent(n -> {
            n.registrarRecepcion(d.cantidadTotal(), momento.toLocalDate());
            necesidades.guardar(n);
        });

        String comprobante = "Comprobante de entrega — Donación: " + d.descripcionCorta()
                + " | Fecha: " + momento.format(FECHA) + " | Hora: " + momento.format(HORA)
                + " | Camión: " + evento.patente() + " | N° " + d.id().toString().substring(0, 8).toUpperCase();
        EntidadBeneficiaria entidad = entidades.buscar(d.entidadId()).orElse(null);
        if (entidad != null) {
            notificador.notificar(entidad.comoDestinatario(), "Recepción confirmada", comprobante, "ENTREGA_REALIZADA");
        }
        donantes.buscar(d.donanteId()).ifPresent(dn -> notificador.notificar(dn.comoDestinatario(),
                "¡Tu donación llegó a destino!",
                "Tu donación fue recibida por " + (entidad == null ? "la entidad" : entidad.razonSocial()) + ". " + comprobante,
                "ENTREGA_REALIZADA"));

        eventos.donacionEntregada(new EventosDonaciones.DonacionEntregada(d.id(), d.donanteId(), d.entidadId(),
                evento.entregadaEn()));
    }
}
