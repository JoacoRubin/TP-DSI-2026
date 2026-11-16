package ar.utn.donatrack.donaciones.asignacion.infrastructure.web;

import ar.utn.donatrack.donaciones.asignacion.application.ActualizarPesos;
import ar.utn.donatrack.donaciones.asignacion.application.ConfirmarAsignacion;
import ar.utn.donatrack.donaciones.asignacion.application.ConsultasAsignacion;
import ar.utn.donatrack.donaciones.asignacion.application.EvaluarDonacion;
import ar.utn.donatrack.donaciones.asignacion.domain.PesosSemanticos;
import ar.utn.donatrack.donaciones.asignacion.domain.Puntaje;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.compartido.infrastructure.TareasProgramadas;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.Solicitantes;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class MatchmakingController {

    private final TareasProgramadas tareas;
    private final EvaluarDonacion evaluar;
    private final ConfirmarAsignacion confirmar;
    private final ConsultasAsignacion consultas;
    private final ActualizarPesos actualizarPesos;
    private final ResultadoMatchmakingRepositorio resultados;
    private final Clock reloj;

    MatchmakingController(TareasProgramadas tareas, EvaluarDonacion evaluar, ConfirmarAsignacion confirmar,
                          ConsultasAsignacion consultas, ActualizarPesos actualizarPesos,
                          ResultadoMatchmakingRepositorio resultados, Clock reloj) {
        this.tareas = tareas;
        this.evaluar = evaluar;
        this.confirmar = confirmar;
        this.consultas = consultas;
        this.actualizarPesos = actualizarPesos;
        this.resultados = resultados;
        this.reloj = reloj;
    }

    record PendienteVista(UUID donacionId, String descripcion, String categoria, String subcategoria, int cantidad,
                          Instant registradaEn, LocalDate fechaVencimiento, boolean evaluada, int coincidencias) {
    }

    record PropuestaVista(int posicion, UUID necesidadId, UUID entidadId, String entidad, String descripcion,
                          String tipo, int pendiente, String unidad, double puntaje) {
    }

    record RankingVista(String algoritmo, List<PropuestaVista> propuestas) {
    }

    record DetalleVista(UUID donacionId, String descripcion, String subcategoria, int cantidad, String estado,
                        Instant ejecutadoEn, List<RankingVista> rankings, List<PropuestaVista> coincidencias) {
    }

    record AsignacionRequest(@NotNull UUID necesidadId) {
    }

    /** Ejecución a demanda de los algoritmos: corre en segundo plano y responde 202. */
    @PostMapping("/api/matchmaking/ejecuciones")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, String> ejecutar() {
        tareas.matchmakingAsincronico();
        return Map.of("estado", "EN_CURSO", "detalle", "La evaluación corre en segundo plano");
    }

    @GetMapping("/api/matchmaking/pendientes")
    List<PendienteVista> pendientes() {
        return consultas.pendientes().stream().map(p -> {
            Donacion d = p.donacion();
            return new PendienteVista(d.id(), d.descripcionCorta(), d.categoriaNombre(), d.subcategoriaNombre(),
                    d.cantidadTotal(), d.registradaEn(), d.fechaVencimiento(), p.evaluada(), p.coincidencias());
        }).toList();
    }

    @GetMapping("/api/matchmaking/donaciones/{id}")
    DetalleVista detalle(@PathVariable UUID id) {
        return aVista(consultas.detalle(id));
    }

    @PostMapping("/api/matchmaking/donaciones/{id}/evaluacion")
    @Transactional
    DetalleVista evaluar(@PathVariable UUID id) {
        evaluar.ejecutar(id);
        return aVista(consultas.detalle(id));
    }

    @PostMapping("/api/donaciones/{id}/asignacion")
    @Transactional
    Map<String, String> asignar(@PathVariable UUID id, @RequestBody AsignacionRequest req, Authentication auth) {
        Donacion d = confirmar.ejecutar(id, req.necesidadId(), Solicitantes.de(auth));
        return Map.of("donacionId", d.id().toString(), "estado", d.estado().name(), "entidadId", d.entidadId().toString());
    }

    @GetMapping("/api/matchmaking/pesos")
    PesosSemanticos pesos() {
        return resultados.pesos();
    }

    @PutMapping("/api/matchmaking/pesos")
    @Transactional
    PesosSemanticos actualizarPesos(@RequestBody PesosSemanticos pesos) {
        return actualizarPesos.ejecutar(pesos.textual(), pesos.volumen(), pesos.tipo());
    }

    private DetalleVista aVista(ConsultasAsignacion.Detalle det) {
        Donacion d = det.donacion();
        LocalDate hoy = LocalDate.now(reloj);
        if (det.resultado().isEmpty()) {
            return new DetalleVista(d.id(), d.descripcionCorta(), d.subcategoriaNombre(), d.cantidadTotal(),
                    d.estado().name(), null, List.of(), List.of());
        }
        ResultadoMatchmaking r = det.resultado().get();
        List<RankingVista> rankings = r.rankings().stream().map(rk -> {
            List<PropuestaVista> ps = new java.util.ArrayList<>();
            for (int i = 0; i < rk.puntajes().size(); i++) {
                ps.add(propuesta(i + 1, rk.puntajes().get(i), det, hoy));
            }
            return new RankingVista(rk.algoritmo(), ps);
        }).toList();
        List<PropuestaVista> coincidencias = new java.util.ArrayList<>();
        for (int i = 0; i < r.coincidencias().size(); i++) {
            UUID nid = r.coincidencias().get(i);
            double promedio = r.rankings().stream().mapToDouble(rk -> rk.puntaje(nid).map(Puntaje::valor).orElse(0.0)).average().orElse(0);
            Necesidad n = det.necesidades().get(nid);
            coincidencias.add(propuesta(i + 1, new Puntaje(nid, n == null ? null : n.entidadId(), promedio), det, hoy));
        }
        return new DetalleVista(d.id(), d.descripcionCorta(), d.subcategoriaNombre(), d.cantidadTotal(), d.estado().name(),
                r.ejecutadoEn(), rankings, coincidencias);
    }

    private static PropuestaVista propuesta(int pos, Puntaje p, ConsultasAsignacion.Detalle det, LocalDate hoy) {
        Necesidad n = det.necesidades().get(p.necesidadId());
        EntidadBeneficiaria e = n == null ? null : det.entidades().get(n.entidadId());
        return new PropuestaVista(pos, p.necesidadId(), p.entidadId(), e == null ? "(sin datos)" : e.razonSocial(),
                n == null ? "(necesidad eliminada)" : n.descripcion(), n == null ? null : n.tipo().name(),
                n == null ? 0 : n.cantidadPendiente(hoy), n == null ? null : n.unidad(),
                Math.round(p.valor() * 1000) / 1000.0);
    }
}
