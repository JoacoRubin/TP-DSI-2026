package ar.utn.donatrack.logistica.compartido.infrastructure.web;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.compartido.infrastructure.AlmacenFotos;
import ar.utn.donatrack.logistica.compartido.infrastructure.web.Vistas.CamionVista;
import ar.utn.donatrack.logistica.compartido.infrastructure.web.Vistas.EntregaPublicaVista;
import ar.utn.donatrack.logistica.compartido.infrastructure.web.Vistas.EntregaVista;
import ar.utn.donatrack.logistica.compartido.infrastructure.web.Vistas.RutaVista;
import ar.utn.donatrack.logistica.compartido.infrastructure.web.Vistas.SolicitudVista;
import ar.utn.donatrack.logistica.entregas.application.AgregarFotos;
import ar.utn.donatrack.logistica.entregas.application.CancelarEntrega;
import ar.utn.donatrack.logistica.entregas.application.ConfirmarRecepcion;
import ar.utn.donatrack.logistica.entregas.application.CrearEntrega;
import ar.utn.donatrack.logistica.entregas.application.InformarNoRecepcion;
import ar.utn.donatrack.logistica.entregas.application.ReprogramarEntrega;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.entregas.domain.ResumenEntregas;
import ar.utn.donatrack.logistica.flota.application.GestionarFlota;
import ar.utn.donatrack.logistica.rutas.application.ConsultasLogistica;
import ar.utn.donatrack.logistica.rutas.application.IniciarRuta;
import ar.utn.donatrack.logistica.rutas.application.PlanificarRutas;
import ar.utn.donatrack.logistica.rutas.application.RegistrarPosicion;
import ar.utn.donatrack.logistica.rutas.application.RegistrarResultadoPlanificacion;
import ar.utn.donatrack.logistica.rutas.domain.Posicion;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class LogisticaController {

    private final CrearEntrega crearEntrega;
    private final CancelarEntrega cancelarEntrega;
    private final ConfirmarRecepcion confirmarRecepcion;
    private final AgregarFotos agregarFotos;
    private final InformarNoRecepcion informarNoRecepcion;
    private final ReprogramarEntrega reprogramar;
    private final PlanificarRutas planificar;
    private final RegistrarResultadoPlanificacion registrarResultado;
    private final IniciarRuta iniciarRuta;
    private final RegistrarPosicion registrarPosicion;
    private final GestionarFlota flota;
    private final ConsultasLogistica consultas;
    private final AlmacenFotos fotos;
    private final String tokenCallback;
    private final Clock reloj;

    LogisticaController(CrearEntrega crearEntrega, CancelarEntrega cancelarEntrega, ConfirmarRecepcion confirmarRecepcion,
                        AgregarFotos agregarFotos, InformarNoRecepcion informarNoRecepcion, ReprogramarEntrega reprogramar,
                        PlanificarRutas planificar, RegistrarResultadoPlanificacion registrarResultado,
                        IniciarRuta iniciarRuta, RegistrarPosicion registrarPosicion, GestionarFlota flota,
                        ConsultasLogistica consultas, AlmacenFotos fotos,
                        @Value("${donatrack.logistica.token-callback}") String tokenCallback, Clock reloj) {
        this.crearEntrega = crearEntrega;
        this.cancelarEntrega = cancelarEntrega;
        this.confirmarRecepcion = confirmarRecepcion;
        this.agregarFotos = agregarFotos;
        this.informarNoRecepcion = informarNoRecepcion;
        this.reprogramar = reprogramar;
        this.planificar = planificar;
        this.registrarResultado = registrarResultado;
        this.iniciarRuta = iniciarRuta;
        this.registrarPosicion = registrarPosicion;
        this.flota = flota;
        this.consultas = consultas;
        this.fotos = fotos;
        this.tokenCallback = tokenCallback;
        this.reloj = reloj;
    }

    // ================================================================ entregas

    @PostMapping("/api/entregas")
    @ResponseStatus(HttpStatus.CREATED)
    EntregaVista crear(@RequestBody CrearEntrega.Comando comando) {
        return EntregaVista.de(crearEntrega.ejecutar(comando));
    }

    @DeleteMapping("/api/entregas/por-donacion/{donacionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelar(@PathVariable UUID donacionId) {
        cancelarEntrega.ejecutar(donacionId, "Cancelada desde el Servicio de Donaciones");
    }

    @GetMapping("/api/entregas")
    List<EntregaVista> entregas(@RequestParam(required = false) EstadoEntrega estado,
                                @RequestParam(required = false) UUID entidadId,
                                @RequestParam(required = false) UUID donanteId,
                                @RequestParam(required = false) List<UUID> donacionIds, Authentication auth) {
        Actor a = Vistas.actor(auth);
        if (donacionIds != null) {
            // Acotado a una página de donaciones; la autorización filtra lo ajeno.
            return consultas.entregasDeDonaciones(donacionIds, a).stream().map(EntregaVista::de).toList();
        }
        if ("BENEFICIARIA".equals(a.rol())) {
            entidadId = UUID.fromString(a.referencia());
        } else if ("DONANTE".equals(a.rol())) {
            donanteId = UUID.fromString(a.referencia());
        } else if (!a.esAdmin()) {
            throw new OperacionNoPermitidaException("Sin acceso a entregas");
        }
        return consultas.entregas(estado, entidadId, donanteId).stream().map(EntregaVista::de).toList();
    }

    @GetMapping("/api/entregas/{id}")
    EntregaVista entrega(@PathVariable UUID id, Authentication auth) {
        Entrega e = consultas.entrega(id);
        Actor a = Vistas.actor(auth);
        if (!a.esAdmin() && !e.entidadId().toString().equals(a.referencia())
                && !(e.donanteId() != null && e.donanteId().toString().equals(a.referencia()))
                && !(e.patente() != null && e.patente().equals(a.referencia()))) {
            throw new OperacionNoPermitidaException("Sin acceso a esta entrega");
        }
        return EntregaVista.de(e);
    }

    @PostMapping(value = "/api/entregas/{id}/recepcion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    EntregaVista confirmarRecepcion(@PathVariable UUID id,
                                    @RequestPart(value = "fotos", required = false) List<MultipartFile> archivos,
                                    Authentication auth) {
        return EntregaVista.de(confirmarRecepcion.ejecutar(id, fotos.guardar(archivos), Vistas.actor(auth)));
    }

    @PostMapping(value = "/api/entregas/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    EntregaVista agregarFotos(@PathVariable UUID id, @RequestPart("fotos") List<MultipartFile> archivos, Authentication auth) {
        return EntregaVista.de(agregarFotos.ejecutar(id, fotos.guardar(archivos), Vistas.actor(auth)));
    }

    record NoRecibidaRequest(@NotBlank String motivo) {
    }

    @PostMapping("/api/entregas/{id}/no-recibida")
    EntregaVista noRecibida(@PathVariable UUID id, @Valid @RequestBody NoRecibidaRequest req, Authentication auth) {
        return EntregaVista.de(informarNoRecepcion.ejecutar(id, req.motivo(), Vistas.actor(auth)));
    }

    @PostMapping("/api/entregas/{id}/reprogramacion")
    EntregaVista reprogramar(@PathVariable UUID id) {
        return EntregaVista.de(reprogramar.ejecutar(id));
    }

    // ================================================================ planificación

    @PostMapping("/api/planificaciones")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, Object> planificar() {
        return Map.of("lotesEnviados", planificar.ejecutar());
    }

    /** URL de callback del planificador externo. Se autentica con un secreto compartido. */
    @PostMapping("/api/planificaciones/callback")
    Map<String, Object> callback(@RequestHeader(value = "X-Callback-Token", required = false) String token,
                                 @RequestBody RegistrarResultadoPlanificacion.Resultado resultado) {
        if (token == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                tokenCallback.getBytes(StandardCharsets.UTF_8))) {
            throw new OperacionNoPermitidaException("Token de callback inválido");
        }
        List<Ruta> rutas = registrarResultado.ejecutar(resultado);
        return Map.of("rutasRegistradas", rutas.size());
    }

    @GetMapping("/api/planificaciones")
    List<SolicitudVista> solicitudes() {
        return consultas.solicitudesRecientes().stream().map(SolicitudVista::de).toList();
    }

    // ================================================================ rutas y seguimiento

    @GetMapping("/api/rutas")
    List<RutaVista> rutas() {
        return consultas.rutasRecientes().stream().map(RutaVista::de).toList();
    }

    @GetMapping("/api/rutas/{id}")
    RutaVista ruta(@PathVariable UUID id, Authentication auth) {
        Actor a = Vistas.actor(auth);
        Ruta r = consultas.ruta(id);
        a.exigirAccesoA(r);
        return RutaVista.filtrada(r, "CHOFER".equals(a.rol()) ? null : a);
    }

    @GetMapping("/api/rutas/{id}/entregas")
    List<EntregaVista> entregasDeRuta(@PathVariable UUID id, Authentication auth) {
        Ruta r = consultas.ruta(id);
        Vistas.actor(auth).exigirChoferDe(r.patente());
        return consultas.entregasDeRuta(id).stream().map(EntregaVista::de).toList();
    }

    @GetMapping("/api/chofer/rutas")
    List<RutaVista> rutasDelChofer(Authentication auth) {
        return consultas.rutasDeChofer(Vistas.actor(auth).referencia()).stream().map(RutaVista::de).toList();
    }

    @PostMapping("/api/rutas/{id}/inicio")
    RutaVista iniciar(@PathVariable UUID id, Authentication auth) {
        return RutaVista.de(iniciarRuta.ejecutar(id, Vistas.actor(auth)));
    }

    record PosicionRequest(@NotNull Double latitud, @NotNull Double longitud, Double velocidadKmh, Instant registradaEn) {
    }

    /** Contrato con la app móvil del chofer. Responde 202 si se aceptó, 200 si se descartó por desorden. */
    @PostMapping("/api/rutas/{id}/posiciones")
    ResponseEntity<Map<String, Object>> posicion(@PathVariable UUID id, @Valid @RequestBody PosicionRequest req,
                                                 Authentication auth) {
        Posicion p = new Posicion(req.latitud(), req.longitud(), req.velocidadKmh() == null ? 0 : req.velocidadKmh(),
                req.registradaEn() == null ? reloj.instant() : req.registradaEn());
        boolean aceptada = registrarPosicion.ejecutar(id, p, Vistas.actor(auth));
        return ResponseEntity.status(aceptada ? HttpStatus.ACCEPTED : HttpStatus.OK).body(Map.of("aceptada", aceptada));
    }

    @GetMapping("/api/seguimiento")
    List<RutaVista> seguimiento(Authentication auth) {
        Actor a = Vistas.actor(auth);
        return consultas.seguimiento(a).stream().map(r -> RutaVista.filtrada(r, a)).toList();
    }

    @GetMapping("/api/monitoreo")
    List<RutaVista> monitoreo() {
        return consultas.monitoreo().stream().map(RutaVista::de).toList();
    }

    // ================================================================ flota

    record CamionRequest(@NotBlank String patente, @Positive double volumenM3, @Positive double alturaM,
                         @Positive double cargaKg, Boolean activo) {
    }

    @GetMapping("/api/camiones")
    List<CamionVista> camiones() {
        return flota.listar().stream().map(CamionVista::de).toList();
    }

    @PostMapping("/api/camiones")
    @ResponseStatus(HttpStatus.CREATED)
    CamionVista registrarCamion(@Valid @RequestBody CamionRequest req) {
        return CamionVista.de(flota.registrar(req.patente(), req.volumenM3(), req.alturaM(), req.cargaKg()));
    }

    @GetMapping("/api/camiones/{id}")
    CamionVista camion(@PathVariable UUID id) {
        return CamionVista.de(flota.buscar(id));
    }

    @PutMapping("/api/camiones/{id}")
    CamionVista actualizarCamion(@PathVariable UUID id, @Valid @RequestBody CamionRequest req) {
        return CamionVista.de(flota.actualizar(id, req.volumenM3(), req.alturaM(), req.cargaKg(),
                req.activo() == null || req.activo()));
    }

    @DeleteMapping("/api/camiones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void bajaCamion(@PathVariable UUID id) {
        flota.darDeBaja(id);
    }

    // ================================================================ público

    @GetMapping("/api/publico/entregas")
    List<EntregaPublicaVista> entregadasRecientes(@RequestParam(defaultValue = "30") int dias,
                                                  @RequestParam(defaultValue = "200") int limite) {
        return consultas.entregadasRecientes(Math.min(dias, 3650), Math.min(limite, 500)).stream()
                .map(EntregaPublicaVista::de).toList();
    }

    /** Métricas públicas reales (sin datos personales): cantidades, no listados. */
    @GetMapping("/api/publico/resumen")
    ResumenEntregas resumenPublico(@RequestParam(defaultValue = "30") int dias) {
        return consultas.resumenPublico(Math.max(1, Math.min(dias, 3650)));
    }

    @GetMapping("/api/fotos/{nombre}")
    ResponseEntity<Resource> foto(@PathVariable String nombre) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .contentType(MediaTypeFactory.getMediaType(nombre).orElse(MediaType.IMAGE_JPEG))
                .body(new FileSystemResource(fotos.resolver(nombre)));
    }
}
