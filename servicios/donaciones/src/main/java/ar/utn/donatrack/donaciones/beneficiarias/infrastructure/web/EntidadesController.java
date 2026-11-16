package ar.utn.donatrack.donaciones.beneficiarias.infrastructure.web;

import ar.utn.donatrack.donaciones.beneficiarias.application.ActualizarEntidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.ActualizarNecesidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.ConsultasBeneficiarias;
import ar.utn.donatrack.donaciones.beneficiarias.application.DarDeBajaEntidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.DarDeBajaNecesidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.RegistrarEntidad;
import ar.utn.donatrack.donaciones.beneficiarias.application.RegistrarNecesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Periodicidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.TipoNecesidad;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.DtosComunes.DireccionDto;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.Solicitantes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
class EntidadesController {

    private final RegistrarEntidad registrar;
    private final ActualizarEntidad actualizar;
    private final DarDeBajaEntidad darDeBaja;
    private final RegistrarNecesidad registrarNecesidad;
    private final ActualizarNecesidad actualizarNecesidad;
    private final DarDeBajaNecesidad darDeBajaNecesidad;
    private final ConsultasBeneficiarias consultas;
    private final Clock reloj;

    EntidadesController(RegistrarEntidad registrar, ActualizarEntidad actualizar, DarDeBajaEntidad darDeBaja,
                        RegistrarNecesidad registrarNecesidad, ActualizarNecesidad actualizarNecesidad,
                        DarDeBajaNecesidad darDeBajaNecesidad, ConsultasBeneficiarias consultas, Clock reloj) {
        this.registrar = registrar;
        this.actualizar = actualizar;
        this.darDeBaja = darDeBaja;
        this.registrarNecesidad = registrarNecesidad;
        this.actualizarNecesidad = actualizarNecesidad;
        this.darDeBajaNecesidad = darDeBajaNecesidad;
        this.consultas = consultas;
        this.reloj = reloj;
    }

    record EntidadRequest(@NotBlank String razonSocial, @Valid @NotNull DireccionDto direccion, @NotBlank String telefono,
                          @NotEmpty List<@Email String> emails, String clave) {
    }

    record EntidadVista(UUID id, String razonSocial, DireccionDto direccion, String direccionFormateada, String telefono,
                        List<String> emails, boolean activa, Instant fechaAlta) {
        static EntidadVista de(EntidadBeneficiaria e) {
            return new EntidadVista(e.id(), e.razonSocial(), DireccionDto.de(e.direccion()), e.direccion().formateada(),
                    e.telefono(), e.emailsRepresentantes(), e.activa(), e.fechaAlta());
        }
    }

    record NecesidadRequest(@NotNull TipoNecesidad tipo, @NotNull UUID subcategoriaId, @NotBlank String descripcion,
                            @Positive int cantidad, String unidad, Periodicidad periodicidad) {
    }

    record ActualizarNecesidadRequest(@NotBlank String descripcion, @Positive int cantidad, String unidad) {
    }

    record NecesidadVista(UUID id, UUID entidadId, String tipo, UUID subcategoriaId, String subcategoria, String descripcion,
                          int cantidadRequerida, String unidad, int cantidadRecibida, int cantidadComprometida,
                          int cantidadPendiente, boolean satisfecha, String periodicidad, boolean activa, Instant creadaEn) {
        static NecesidadVista de(Necesidad n, LocalDate hoy) {
            return new NecesidadVista(n.id(), n.entidadId(), n.tipo().name(), n.subcategoriaId(), n.subcategoriaNombre(),
                    n.descripcion(), n.cantidadRequerida(), n.unidad(), n.cantidadRecibida(hoy), n.cantidadComprometida(),
                    n.cantidadPendiente(hoy), n.estaSatisfecha(hoy),
                    n instanceof NecesidadRecurrente r ? r.periodicidad().name() : null, n.activa(), n.creadaEn());
        }
    }

    // ------------------------------------------------------------------ entidades

    @PostMapping("/api/entidades")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    EntidadVista registrar(@Valid @RequestBody EntidadRequest req) {
        return EntidadVista.de(registrar.ejecutar(new RegistrarEntidad.Comando(req.razonSocial(), req.direccion().aDominio(),
                req.telefono(), req.emails(), null)));
    }

    @PostMapping("/api/registro/entidades")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    EntidadVista autorregistro(@Valid @RequestBody EntidadRequest req) {
        if (req.clave() == null || req.clave().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria para registrarse");
        }
        return EntidadVista.de(registrar.ejecutar(new RegistrarEntidad.Comando(req.razonSocial(), req.direccion().aDominio(),
                req.telefono(), req.emails(), req.clave())));
    }

    @GetMapping("/api/entidades")
    List<EntidadVista> listar(@RequestParam(required = false) String texto) {
        return consultas.listar(texto).stream().map(EntidadVista::de).toList();
    }

    @GetMapping("/api/entidades/{id}")
    EntidadVista detalle(@PathVariable UUID id) {
        return EntidadVista.de(consultas.detalle(id));
    }

    @PutMapping("/api/entidades/{id}")
    @Transactional
    EntidadVista actualizar(@PathVariable UUID id, @Valid @RequestBody EntidadRequest req, Authentication auth) {
        return EntidadVista.de(actualizar.ejecutar(id, req.razonSocial(), req.direccion().aDominio(), req.telefono(),
                req.emails(), Solicitantes.de(auth)));
    }

    @DeleteMapping("/api/entidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void darDeBaja(@PathVariable UUID id) {
        darDeBaja.ejecutar(id);
    }

    // ------------------------------------------------------------------ necesidades

    @GetMapping("/api/entidades/{id}/necesidades")
    List<NecesidadVista> necesidades(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        LocalDate hoy = LocalDate.now(reloj);
        return consultas.necesidadesDe(id, incluirInactivas).stream().map(n -> NecesidadVista.de(n, hoy)).toList();
    }

    @PostMapping("/api/entidades/{id}/necesidades")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    NecesidadVista registrarNecesidad(@PathVariable UUID id, @Valid @RequestBody NecesidadRequest req, Authentication auth) {
        Necesidad n = registrarNecesidad.ejecutar(new RegistrarNecesidad.Comando(id, req.tipo(), req.subcategoriaId(),
                req.descripcion(), req.cantidad(), req.unidad(), req.periodicidad()), Solicitantes.de(auth));
        return NecesidadVista.de(n, LocalDate.now(reloj));
    }

    @PutMapping("/api/necesidades/{id}")
    @Transactional
    NecesidadVista actualizarNecesidad(@PathVariable UUID id, @Valid @RequestBody ActualizarNecesidadRequest req,
                                       Authentication auth) {
        return NecesidadVista.de(actualizarNecesidad.ejecutar(id, req.descripcion(), req.cantidad(), req.unidad(),
                Solicitantes.de(auth)), LocalDate.now(reloj));
    }

    @DeleteMapping("/api/necesidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void darDeBajaNecesidad(@PathVariable UUID id, Authentication auth) {
        darDeBajaNecesidad.ejecutar(id, Solicitantes.de(auth));
    }
}
