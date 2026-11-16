package ar.utn.donatrack.donaciones;

import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.donaciones.asignacion.domain.PesosSemanticos;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.compartido.application.PublicadorEventos;
import ar.utn.donatrack.donaciones.compartido.application.ServicioCuentas;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import ar.utn.donatrack.donaciones.compartido.application.Transaccion;
import ar.utn.donatrack.donaciones.compartido.domain.Pagina;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.FiltroDonaciones;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import ar.utn.donatrack.donaciones.importacion.domain.ImportacionRepositorio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Fakes en memoria de los puertos: permiten testear casos de uso sin infraestructura. */
public final class EnMemoria {

    private EnMemoria() {
    }

    public static class Donantes implements DonanteRepositorio {
        public final Map<UUID, Donante> datos = new LinkedHashMap<>();
        public int consultasPorEmails;
        public Optional<Donante> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public Optional<Donante> buscarPorEmail(String e) { return datos.values().stream().filter(d -> d.email().equals(e)).findFirst(); }
        public List<Donante> buscarPorEmails(Collection<String> es) {
            consultasPorEmails++;
            return datos.values().stream().filter(d -> es.contains(d.email())).toList();
        }
        public boolean existeEmail(String e) { return buscarPorEmail(e).isPresent(); }
        public void guardar(Donante d) { datos.put(d.id(), d); }
        public void guardarTodos(Collection<? extends Donante> ds) { ds.forEach(this::guardar); }
        public Pagina<Donante> listar(String t, int p, int s) { return new Pagina<>(List.copyOf(datos.values()), p, s, datos.size()); }
        public List<Donante> sinInteraccionDesde(Instant limite) {
            return datos.values().stream().filter(d -> d.ultimaInteraccion().isBefore(limite)).toList();
        }
    }

    public static class Entidades implements EntidadRepositorio {
        public final Map<UUID, EntidadBeneficiaria> datos = new LinkedHashMap<>();
        public Optional<EntidadBeneficiaria> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public Map<UUID, EntidadBeneficiaria> buscarTodas(Collection<UUID> ids) {
            return ids.stream().map(datos::get).filter(Objects::nonNull).collect(Collectors.toMap(EntidadBeneficiaria::id, e -> e, (a, b) -> a));
        }
        public boolean existeEmail(String e) { return datos.values().stream().anyMatch(x -> x.emailsRepresentantes().contains(e)); }
        public void guardar(EntidadBeneficiaria e) { datos.put(e.id(), e); }
        public List<EntidadBeneficiaria> listarActivas(String t) { return List.copyOf(datos.values()); }
    }

    public static class Necesidades implements NecesidadRepositorio {
        public final Map<UUID, Necesidad> datos = new LinkedHashMap<>();
        public Optional<Necesidad> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public void guardar(Necesidad n) { datos.put(n.id(), n); }
        public List<Necesidad> deEntidad(UUID id) { return datos.values().stream().filter(n -> n.entidadId().equals(id)).toList(); }
        public List<Necesidad> activasDeSubcategoria(UUID s) {
            return datos.values().stream().filter(n -> n.activa() && n.subcategoriaId().equals(s)).toList();
        }
    }

    public static class Donaciones implements DonacionRepositorio {
        public final Map<UUID, Donacion> datos = new LinkedHashMap<>();
        public Optional<Donacion> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public List<Donacion> buscarTodas(Collection<UUID> ids) { return ids.stream().map(datos::get).filter(Objects::nonNull).toList(); }
        public void guardar(Donacion d) { datos.put(d.id(), d); }
        public void guardarTodas(Collection<Donacion> ds) { ds.forEach(this::guardar); }
        public void eliminar(UUID id) { datos.remove(id); }
        public Pagina<Donacion> buscar(FiltroDonaciones f, int p, int s) { return new Pagina<>(List.copyOf(datos.values()), p, s, datos.size()); }
        public List<Donacion> enEstado(EstadoDonacion e) { return datos.values().stream().filter(d -> d.estado() == e).toList(); }
        public Map<UUID, Integer> entregadasPorEntidadDesde(Instant desde) { return Map.of(); }
        public List<Donacion> entregadasDesde(Instant desde, int limite) { return List.of(); }
    }

    public static class Resultados implements ResultadoMatchmakingRepositorio {
        public final Map<UUID, ResultadoMatchmaking> datos = new HashMap<>();
        public PesosSemanticos pesos = PesosSemanticos.PREDETERMINADOS;
        public void guardar(ResultadoMatchmaking r) { datos.put(r.donacionId(), r); }
        public Optional<ResultadoMatchmaking> deDonacion(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public void eliminarDeDonacion(UUID id) { datos.remove(id); }
        public PesosSemanticos pesos() { return pesos; }
        public void guardarPesos(PesosSemanticos p) { pesos = p; }
    }

    public static class Importaciones implements ImportacionRepositorio {
        public final Map<UUID, Importacion> datos = new HashMap<>();
        public void guardar(Importacion i) { datos.put(i.id(), i); }
        public Optional<Importacion> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public List<Importacion> recientes(int l) { return List.copyOf(datos.values()); }
    }

    public record Enviada(Destinatario destinatario, String asunto, String mensaje, String tipo) {
    }

    public static class NotificadorFalso implements Notificador {
        public final List<Enviada> enviadas = new ArrayList<>();
        public void notificar(Destinatario d, String a, String m, String t) { enviadas.add(new Enviada(d, a, m, t)); }
        public List<Enviada> deTipo(String tipo) { return enviadas.stream().filter(e -> e.tipo().equals(tipo)).toList(); }
    }

    public static class LogisticaFalsa implements ServicioLogistica {
        public final List<SolicitudEntrega> solicitudes = new ArrayList<>();
        public final List<UUID> canceladas = new ArrayList<>();
        public boolean caida;
        public void solicitarEntrega(SolicitudEntrega s) {
            if (caida) throw new IllegalStateException("Logística no disponible");
            solicitudes.add(s);
        }
        public void cancelarEntrega(UUID id) { canceladas.add(id); }
    }

    public static class CuentasFalsas implements ServicioCuentas {
        public final Map<String, NuevaCuenta> cuentas = new HashMap<>();
        public int llamadasLote;
        public CuentaCreada crear(NuevaCuenta c) {
            boolean nueva = cuentas.putIfAbsent(c.email(), c) == null;
            return new CuentaCreada(c.email(), nueva, nueva && c.clave() == null ? "tok-" + c.email() : null);
        }
        public List<CuentaCreada> crearEnLote(List<NuevaCuenta> cs) {
            llamadasLote++;
            return cs.stream().map(this::crear).toList();
        }
    }

    public static class EventosFalsos implements PublicadorEventos {
        public final List<Object> publicados = new ArrayList<>();
        public void donacionRegistrada(EventosDonaciones.DonacionRegistrada e) { publicados.add(e); }
        public void donacionEntregada(EventosDonaciones.DonacionEntregada e) { publicados.add(e); }
    }

    public static final Transaccion SIN_TRANSACCION = new Transaccion() {
        public <T> T ejecutar(Supplier<T> trabajo) { return trabajo.get(); }
    };
}
