package ar.utn.donatrack.logistica;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.compartido.application.PublicadorEventos;
import ar.utn.donatrack.logistica.entregas.application.CierreDeRuta;
import ar.utn.donatrack.logistica.entregas.application.ConfirmarRecepcion;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.flota.domain.CamionRepositorio;
import ar.utn.donatrack.logistica.rutas.application.IniciarRuta;
import ar.utn.donatrack.logistica.rutas.application.PlanificadorRutas;
import ar.utn.donatrack.logistica.rutas.application.PlanificarRutas;
import ar.utn.donatrack.logistica.rutas.application.RegistrarResultadoPlanificacion;
import ar.utn.donatrack.logistica.rutas.domain.EstadoRuta;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import ar.utn.donatrack.logistica.rutas.domain.RutaRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacion;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacionRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static ar.utn.donatrack.logistica.DominioLogisticaTest.AHORA;
import static ar.utn.donatrack.logistica.DominioLogisticaTest.entrega;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CasosDeUsoLogisticaTest {

    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final Entregas entregas = new Entregas();
    private final Camiones camiones = new Camiones();
    private final Rutas rutas = new Rutas();
    private final Solicitudes solicitudes = new Solicitudes();
    private final Eventos eventos = new Eventos();
    private final List<PlanificadorRutas.Pedido> pedidos = new ArrayList<>();
    private Camion camion;

    @BeforeEach
    void preparar() {
        camion = Camion.registrar(UUID.randomUUID(), "AE123BC", 20, 3, 3500);
        camiones.guardar(camion);
    }

    private PlanificarRutas planificar() {
        return new PlanificarRutas(entregas, camiones, solicitudes, pedidos::add,
                new PlanificadorRutas.Ubicacion(-34.5986, -58.4202), reloj);
    }

    private RegistrarResultadoPlanificacion callback() {
        return new RegistrarResultadoPlanificacion(solicitudes, entregas, camiones, rutas, eventos, reloj);
    }

    @Test
    void planificar250EntregasGeneraTresLotesRespetandoElMaximoDe100() {
        IntStream.range(0, 250).forEach(i -> entregas.guardar(entrega(UUID.randomUUID())));

        assertThat(planificar().ejecutar()).isEqualTo(3);

        assertThat(pedidos).extracting(p -> p.entregas().size()).containsExactly(100, 100, 50);
        assertThat(entregas.datos.values()).allSatisfy(e -> assertThat(e.estado()).isEqualTo(EstadoEntrega.EN_PLANIFICACION));
        assertThat(planificar().ejecutar()).as("no reenvía lo que ya está en planificación").isZero();
    }

    @Test
    void elCallbackCreaRutasYDevuelveAPendienteLasNoAsignadas() {
        Entrega a = entrega(UUID.randomUUID());
        Entrega b = entrega(UUID.randomUUID());
        entregas.guardar(a);
        entregas.guardar(b);
        planificar().ejecutar();
        UUID solicitudId = pedidos.get(0).solicitudId();

        List<Ruta> generadas = callback().ejecutar(new RegistrarResultadoPlanificacion.Resultado(solicitudId,
                List.of(new RegistrarResultadoPlanificacion.RutaPropuesta(camion.id(),
                        List.of(new RegistrarResultadoPlanificacion.ParadaPropuesta(1, List.of(a.id()))))),
                List.of(b.id())));

        assertThat(generadas).hasSize(1);
        assertThat(a.estado()).isEqualTo(EstadoEntrega.PLANIFICADA);
        assertThat(b.estado()).isEqualTo(EstadoEntrega.PENDIENTE);
        assertThat(eventos.publicados).singleElement().isInstanceOf(EventosLogistica.EntregaPlanificada.class);
        assertThat(solicitudes.datos.get(solicitudId).sinAsignar()).isEqualTo(1);

        assertThat(callback().ejecutar(new RegistrarResultadoPlanificacion.Resultado(solicitudId, List.of(), List.of())))
                .as("callback repetido es idempotente").isEmpty();
    }

    private Ruta rutaPlanificada(Entrega e) {
        entregas.guardar(e);
        planificar().ejecutar();
        return callback().ejecutar(new RegistrarResultadoPlanificacion.Resultado(pedidos.get(pedidos.size() - 1).solicitudId(),
                List.of(new RegistrarResultadoPlanificacion.RutaPropuesta(camion.id(),
                        List.of(new RegistrarResultadoPlanificacion.ParadaPropuesta(1, List.of(e.id()))))), List.of())).get(0);
    }

    @Test
    void soloElChoferDelCamionPuedeIniciarLaRutaYSeNotificanLasDonaciones() {
        Entrega e = entrega(UUID.randomUUID());
        Ruta ruta = rutaPlanificada(e);
        IniciarRuta iniciar = new IniciarRuta(rutas, entregas, eventos, "http://front/seguimiento", reloj);

        assertThatThrownBy(() -> iniciar.ejecutar(ruta.id(), new Actor("otro", "CHOFER", "ZZ999ZZ")))
                .isInstanceOf(OperacionNoPermitidaException.class);

        iniciar.ejecutar(ruta.id(), new Actor("chofer", "CHOFER", "AE123BC"));
        assertThat(ruta.estado()).isEqualTo(EstadoRuta.EN_CURSO);
        assertThat(e.estado()).isEqualTo(EstadoEntrega.EN_TRASLADO);
        assertThat(eventos.publicados).last().isInstanceOfSatisfying(EventosLogistica.RutaIniciada.class,
                ev -> assertThat(ev.donacionIds()).containsExactly(e.donacionId()));
    }

    @Test
    void laEntidadConfirmaRecepcionYLaRutaSeFinalizaAlCompletarse() {
        Entrega e = entrega(UUID.randomUUID());
        Ruta ruta = rutaPlanificada(e);
        new IniciarRuta(rutas, entregas, eventos, "url", reloj).ejecutar(ruta.id(), Actor.SISTEMA);
        ConfirmarRecepcion confirmar = new ConfirmarRecepcion(entregas, new CierreDeRuta(entregas, rutas), eventos, reloj);

        assertThatThrownBy(() -> confirmar.ejecutar(e.id(), List.of(), new Actor("x", "BENEFICIARIA", UUID.randomUUID().toString())))
                .isInstanceOf(OperacionNoPermitidaException.class);

        confirmar.ejecutar(e.id(), List.of("/api/fotos/1.jpg"), new Actor("escuela", "BENEFICIARIA", e.entidadId().toString()));

        assertThat(e.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(ruta.estado()).isEqualTo(EstadoRuta.FINALIZADA);
        assertThat(eventos.publicados).last().isInstanceOfSatisfying(EventosLogistica.EntregaRealizada.class,
                ev -> assertThat(ev.patente()).isEqualTo("AE123BC"));
    }

    @Test
    void siElPlanificadorFallaElLoteVuelveAPendiente() {
        Entrega e = entrega(UUID.randomUUID());
        entregas.guardar(e);
        PlanificarRutas conFalla = new PlanificarRutas(entregas, camiones, solicitudes,
                p -> { throw new IllegalStateException("timeout"); }, new PlanificadorRutas.Ubicacion(0, 0), reloj);

        assertThatThrownBy(conFalla::ejecutar).isInstanceOf(IllegalStateException.class);
        assertThat(e.estado()).isEqualTo(EstadoEntrega.PENDIENTE);
        assertThat(solicitudes.datos.values()).singleElement()
                .extracting(SolicitudPlanificacion::estado).isEqualTo(SolicitudPlanificacion.Estado.FALLIDA);
    }

    // ------------------------------------------------------------------ fakes

    static class Entregas implements EntregaRepositorio {
        final Map<UUID, Entrega> datos = new LinkedHashMap<>();
        public Optional<Entrega> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public Optional<Entrega> buscarPorDonacion(UUID id) { return datos.values().stream().filter(e -> e.donacionId().equals(id)).findFirst(); }
        public List<Entrega> buscarTodas(Collection<UUID> ids) { return ids.stream().map(datos::get).filter(Objects::nonNull).toList(); }
        public List<Entrega> enEstado(EstadoEntrega e) { return datos.values().stream().filter(x -> x.estado() == e).toList(); }
        public List<Entrega> deRuta(UUID r) { return datos.values().stream().filter(x -> r.equals(x.rutaId())).toList(); }
        public List<Entrega> filtrar(EstadoEntrega e, UUID en, UUID d) { return List.copyOf(datos.values()); }
        public List<Entrega> entregadasDesde(Instant desde, int l) { return List.of(); }
        public void guardar(Entrega e) { datos.put(e.id(), e); }
        public void guardarTodas(Collection<Entrega> es) { es.forEach(this::guardar); }
        public ar.utn.donatrack.logistica.entregas.domain.Pagina<Entrega> buscar(ar.utn.donatrack.logistica.entregas.domain.FiltroEntregas f, int n, int t) {
            List<Entrega> todas = datos.values().stream().filter(e -> f.estados().isEmpty() || f.estados().contains(e.estado())).toList();
            return new ar.utn.donatrack.logistica.entregas.domain.Pagina<>(todas.stream().skip((long) n * t).limit(t).toList(), todas.size(), n, t);
        }
        public List<Entrega> deDonaciones(Collection<UUID> ids) { return datos.values().stream().filter(e -> ids.contains(e.donacionId())).toList(); }
        public ar.utn.donatrack.logistica.entregas.domain.ResumenEntregas resumenEntregadasDesde(Instant desde) { return new ar.utn.donatrack.logistica.entregas.domain.ResumenEntregas(0, 0); }
        public synchronized Entrega registrarSiNoHayActiva(Entrega nueva) {
            return datos.values().stream()
                    .filter(e -> e.donacionId().equals(nueva.donacionId()) && e.estado() != EstadoEntrega.CANCELADA)
                    .findFirst().orElseGet(() -> { guardar(nueva); return nueva; });
        }
    }

    static class Camiones implements CamionRepositorio {
        final Map<UUID, Camion> datos = new LinkedHashMap<>();
        public Optional<Camion> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public Optional<Camion> buscarPorPatente(String p) { return datos.values().stream().filter(c -> c.patente().equals(p)).findFirst(); }
        public List<Camion> todos() { return List.copyOf(datos.values()); }
        public List<Camion> activos() { return datos.values().stream().filter(Camion::activo).toList(); }
        public void guardar(Camion c) { datos.put(c.id(), c); }
    }

    static class Rutas implements RutaRepositorio {
        final Map<UUID, Ruta> datos = new LinkedHashMap<>();
        public Optional<Ruta> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public void guardar(Ruta r) { datos.put(r.id(), r); }
        public void guardarPosicion(Ruta r) { datos.put(r.id(), r); }
        public List<Ruta> enEstados(List<EstadoRuta> es) { return datos.values().stream().filter(r -> es.contains(r.estado())).toList(); }
        public List<Ruta> activasDeDonante(UUID d) { return datos.values().stream().filter(r -> r.involucraDonante(d)).toList(); }
        public List<Ruta> activasDeEntidad(UUID e) { return datos.values().stream().filter(r -> r.involucraEntidad(e)).toList(); }
        public List<Ruta> dePatente(String p, List<EstadoRuta> es) { return List.copyOf(datos.values()); }
        public List<Ruta> recientes(int l) { return List.copyOf(datos.values()); }
    }

    static class Solicitudes implements SolicitudPlanificacionRepositorio {
        final Map<UUID, SolicitudPlanificacion> datos = new LinkedHashMap<>();
        public Optional<SolicitudPlanificacion> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public void guardar(SolicitudPlanificacion s) { datos.put(s.id(), s); }
        public List<SolicitudPlanificacion> recientes(int l) { return List.copyOf(datos.values()); }
    }

    static class Eventos implements PublicadorEventos {
        final List<Object> publicados = new ArrayList<>();
        public void publicar(EventosLogistica.EntregaPlanificada e) { publicados.add(e); }
        public void publicar(EventosLogistica.RutaIniciada e) { publicados.add(e); }
        public void publicar(EventosLogistica.EntregaRealizada e) { publicados.add(e); }
        public void publicar(EventosLogistica.EntregaNoSatisfactoria e) { publicados.add(e); }
        public void publicar(EventosLogistica.EntregaReprogramada e) { publicados.add(e); }
    }
}
