package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.Enlaces;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * El chofer inició la ruta: las donaciones pasan a "En traslado" y se notifica a entidades y
 * donantes involucrados con el enlace al mapa de seguimiento (una notificación por persona).
 */
public class RegistrarInicioDeRuta {

    private final DonacionRepositorio donaciones;
    private final DonanteRepositorio donantes;
    private final EntidadRepositorio entidades;
    private final Notificador notificador;
    private final Enlaces enlaces;

    public RegistrarInicioDeRuta(DonacionRepositorio donaciones, DonanteRepositorio donantes,
                                 EntidadRepositorio entidades, Notificador notificador, Enlaces enlaces) {
        this.donaciones = donaciones;
        this.donantes = donantes;
        this.entidades = entidades;
        this.notificador = notificador;
        this.enlaces = enlaces;
    }

    public void ejecutar(EventosLogistica.RutaIniciada evento) {
        List<Donacion> enRuta = donaciones.buscarTodas(evento.donacionIds()).stream()
                .filter(d -> d.estado() == EstadoDonacion.LISTA_PARA_ENTREGAR || d.estado() == EstadoDonacion.ASIGNADA)
                .toList();
        enRuta.forEach(d -> {
            if (d.estado() == EstadoDonacion.ASIGNADA) {
                // Tolerancia a eventos perdidos/desordenados: si no llegó "EntregaPlanificada",
                // la ruta iniciada lo implica. Se registra igual en la auditoría.
                d.marcarListaParaEntregar(evento.iniciadaEn());
            }
            d.iniciarTraslado(evento.patente(), evento.iniciadaEn());
            donaciones.guardar(d);
        });

        Map<UUID, List<Donacion>> porEntidad = enRuta.stream()
                .collect(Collectors.groupingBy(Donacion::entidadId, LinkedHashMap::new, Collectors.toList()));
        porEntidad.forEach((entidadId, lista) -> entidades.buscar(entidadId).ifPresent(e ->
                notificador.notificar(e.comoDestinatario(), "Tu donación está en camino",
                        "El camión " + evento.patente() + " salió del depósito con " + describir(lista)
                                + ". Seguilo en tiempo real: " + enlaces.seguimientoEntidad(), "RUTA_INICIADA")));

        Map<UUID, List<Donacion>> porDonante = enRuta.stream()
                .collect(Collectors.groupingBy(Donacion::donanteId, LinkedHashMap::new, Collectors.toList()));
        porDonante.forEach((donanteId, lista) -> donantes.buscar(donanteId).ifPresent(dn ->
                notificador.notificar(dn.comoDestinatario(), "Tu donación salió hacia su destino",
                        "¡Gracias " + dn.nombreVisible() + "! " + describir(lista) + " ya viaja en el camión "
                                + evento.patente() + ". Seguilo en el mapa: " + enlaces.seguimientoDonante(), "RUTA_INICIADA")));
    }

    private static String describir(List<Donacion> lista) {
        return lista.stream().map(Donacion::descripcionCorta).collect(Collectors.joining(", "));
    }
}
