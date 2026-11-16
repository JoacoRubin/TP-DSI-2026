package ar.utn.donatrack.logistica.rutas.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.compartido.application.PublicadorEventos;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import ar.utn.donatrack.logistica.rutas.domain.RutaRepositorio;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** El chofer indica que inicia el recorrido: todas sus entregas pasan a "En traslado". */
public class IniciarRuta {

    private final RutaRepositorio rutas;
    private final EntregaRepositorio entregas;
    private final PublicadorEventos eventos;
    private final String urlSeguimiento;
    private final Clock reloj;

    public IniciarRuta(RutaRepositorio rutas, EntregaRepositorio entregas, PublicadorEventos eventos,
                       String urlSeguimiento, Clock reloj) {
        this.rutas = rutas;
        this.entregas = entregas;
        this.eventos = eventos;
        this.urlSeguimiento = urlSeguimiento;
        this.reloj = reloj;
    }

    public Ruta ejecutar(UUID rutaId, Actor actor) {
        Ruta ruta = rutas.buscar(rutaId).orElseThrow(() -> new RecursoNoEncontradoException("Ruta", rutaId));
        actor.exigirChoferDe(ruta.patente());
        Instant ahora = reloj.instant();
        ruta.iniciar(ahora);
        List<Entrega> enViaje = entregas.buscarTodas(ruta.entregaIds()).stream()
                .filter(e -> e.estado() == EstadoEntrega.PLANIFICADA)
                .toList();
        enViaje.forEach(e -> e.iniciarTraslado(ahora));
        entregas.guardarTodas(enViaje);
        rutas.guardar(ruta);
        eventos.publicar(new EventosLogistica.RutaIniciada(ruta.id(), ruta.patente(),
                enViaje.stream().map(Entrega::donacionId).toList(), urlSeguimiento, ahora));
        return ruta;
    }
}
