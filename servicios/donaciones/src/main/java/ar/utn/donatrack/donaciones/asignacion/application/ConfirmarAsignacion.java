package ar.utn.donatrack.donaciones.asignacion.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.Enlaces;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * La persona administradora elige la entidad beneficiaria final entre las propuestas por los
 * algoritmos. Efectos: donación ASIGNADA, cantidad comprometida en la necesidad, solicitud de
 * entrega a Logística y notificaciones a entidad y donante.
 */
public class ConfirmarAsignacion {

    private final DonacionRepositorio donaciones;
    private final NecesidadRepositorio necesidades;
    private final EntidadRepositorio entidades;
    private final DonanteRepositorio donantes;
    private final ResultadoMatchmakingRepositorio resultados;
    private final ServicioLogistica logistica;
    private final Notificador notificador;
    private final Enlaces enlaces;
    private final Clock reloj;

    public ConfirmarAsignacion(DonacionRepositorio donaciones, NecesidadRepositorio necesidades,
                               EntidadRepositorio entidades, DonanteRepositorio donantes,
                               ResultadoMatchmakingRepositorio resultados, ServicioLogistica logistica,
                               Notificador notificador, Enlaces enlaces, Clock reloj) {
        this.donaciones = donaciones;
        this.necesidades = necesidades;
        this.entidades = entidades;
        this.donantes = donantes;
        this.resultados = resultados;
        this.logistica = logistica;
        this.notificador = notificador;
        this.enlaces = enlaces;
        this.reloj = reloj;
    }

    public Donacion ejecutar(UUID donacionId, UUID necesidadId, Solicitante solicitante) {
        Donacion d = donaciones.buscar(donacionId).orElseThrow(() -> new RecursoNoEncontradoException("Donación", donacionId));
        if (d.estado() != EstadoDonacion.EN_DEPOSITO) {
            throw new ReglaDeNegocioException("La donación ya no está disponible para asignar");
        }
        ResultadoMatchmaking resultado = resultados.deDonacion(donacionId)
                .orElseThrow(() -> new ReglaDeNegocioException("Primero ejecutá los algoritmos de asignación"));
        if (!resultado.propone(necesidadId)) {
            throw new ReglaDeNegocioException("La necesidad elegida no figura entre las propuestas por los algoritmos");
        }
        Necesidad n = necesidades.buscar(necesidadId).orElseThrow(() -> new RecursoNoEncontradoException("Necesidad", necesidadId));
        if (!n.admiteDonaciones(LocalDate.now(reloj))) {
            throw new ReglaDeNegocioException("La necesidad ya fue cubierta o dada de baja: volvé a ejecutar los algoritmos");
        }
        EntidadBeneficiaria entidad = entidades.buscar(n.entidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Entidad", n.entidadId()));
        Donante donante = donantes.buscar(d.donanteId()).orElseThrow(() -> new RecursoNoEncontradoException("Donante", d.donanteId()));

        d.asignarA(n.id(), entidad.id(), solicitante.usuario(), reloj.instant());
        n.comprometer(d.cantidadTotal());
        donaciones.guardar(d);
        necesidades.guardar(n);
        resultados.eliminarDeDonacion(donacionId);

        logistica.solicitarEntrega(new ServicioLogistica.SolicitudEntrega(d.id(), donante.id(), donante.nombreVisible(),
                entidad.id(), entidad.razonSocial(), entidad.direccion().formateada(),
                entidad.direccion().coordenadas().latitud(), entidad.direccion().coordenadas().longitud(),
                d.descripcionCorta(), d.subcategoriaNombre(), d.cantidadTotal(), d.unidad(), d.pesoKg(), d.volumenM3()));

        notificador.notificar(entidad.comoDestinatario(), "Se te asignó una donación",
                "Recibirás " + d.descripcionCorta() + " para tu necesidad \"" + n.descripcion() + "\". "
                        + "Te avisaremos cuando el camión salga hacia tu dirección.", "DONACION_ASIGNADA");
        notificador.notificar(donante.comoDestinatario(), "¡Tu donación ya tiene destino!",
                "Tu donación de " + d.descripcionCorta() + " fue asignada a " + entidad.razonSocial()
                        + ". Podés seguir su estado en " + enlaces.misDonaciones(), "DONACION_ASIGNADA");
        return d;
    }
}
