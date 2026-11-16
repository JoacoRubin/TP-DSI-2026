package ar.utn.donatrack.donaciones.donantes.infrastructure.web;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.DtosComunes.DireccionDto;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.DtosComunes.PaginaDto;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.Solicitantes;
import ar.utn.donatrack.donaciones.donantes.application.ActualizarContactoDonante;
import ar.utn.donatrack.donaciones.donantes.application.ConsultasDonantes;
import ar.utn.donatrack.donaciones.donantes.application.DarDeBajaDonante;
import ar.utn.donatrack.donaciones.donantes.application.RegistrarInteraccion;
import ar.utn.donatrack.donaciones.donantes.application.RegistrarPersonaHumana;
import ar.utn.donatrack.donaciones.donantes.application.RegistrarPersonaJuridica;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaJuridica;
import ar.utn.donatrack.donaciones.donantes.domain.Representante;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import ar.utn.donatrack.donaciones.donantes.domain.TipoPersonaJuridica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
class DonantesController {

    private final RegistrarPersonaHumana registrarHumana;
    private final RegistrarPersonaJuridica registrarJuridica;
    private final ActualizarContactoDonante actualizarContacto;
    private final DarDeBajaDonante darDeBaja;
    private final RegistrarInteraccion registrarInteraccion;
    private final ConsultasDonantes consultas;

    DonantesController(RegistrarPersonaHumana registrarHumana, RegistrarPersonaJuridica registrarJuridica,
                       ActualizarContactoDonante actualizarContacto, DarDeBajaDonante darDeBaja,
                       RegistrarInteraccion registrarInteraccion, ConsultasDonantes consultas) {
        this.registrarHumana = registrarHumana;
        this.registrarJuridica = registrarJuridica;
        this.actualizarContacto = actualizarContacto;
        this.darDeBaja = darDeBaja;
        this.registrarInteraccion = registrarInteraccion;
        this.consultas = consultas;
    }

    // ------------------------------------------------------------------ DTOs

    record ContactoDto(@NotBlank @Email String email, String telefono, String whatsapp, MedioNotificacion medioPreferido) {
        MediosContacto aDominio() {
            return new MediosContacto(email, telefono, whatsapp, medioPreferido);
        }
    }

    record PersonaHumanaRequest(@NotBlank String nombre, @NotBlank String apellido, @NotNull Integer edad,
                                @NotNull TipoDocumento tipoDocumento, @NotBlank String numeroDocumento,
                                @NotNull Genero genero, @Valid @NotNull DireccionDto direccion,
                                @Valid @NotNull ContactoDto contacto, String clave) {
        RegistrarPersonaHumana.Comando aComando(String claveEfectiva) {
            return new RegistrarPersonaHumana.Comando(nombre, apellido, edad, new Documento(tipoDocumento, numeroDocumento),
                    genero, direccion.aDominio(), contacto.aDominio(), claveEfectiva);
        }
    }

    record RepresentanteDto(@NotBlank String nombre, @NotBlank @Email String email) {
    }

    record PersonaJuridicaRequest(@NotBlank String razonSocial, @NotNull TipoPersonaJuridica tipo, @NotBlank String rubro,
                                  @NotBlank String cuit, @Valid DireccionDto direccion, @Valid @NotNull ContactoDto contacto,
                                  List<@Valid RepresentanteDto> representantes, String clave) {
        RegistrarPersonaJuridica.Comando aComando(String claveEfectiva) {
            return new RegistrarPersonaJuridica.Comando(razonSocial, tipo, rubro, new Documento(TipoDocumento.CUIT, cuit),
                    direccion == null ? null : direccion.aDominio(), contacto.aDominio(),
                    representantes == null ? List.of() : representantes.stream().map(r -> new Representante(r.nombre(), r.email())).toList(),
                    claveEfectiva);
        }
    }

    record ActualizarContactoRequest(@Valid @NotNull ContactoDto contacto, @Valid DireccionDto direccion) {
    }

    record DonanteVista(UUID id, String tipo, String nombreVisible, String email, String telefono, String whatsapp,
                        String medioPreferido, String tipoDocumento, String numeroDocumento, DireccionDto direccion,
                        boolean activo, Instant fechaAlta, Instant ultimaInteraccion,
                        String nombre, String apellido, Integer edad, String genero,
                        String razonSocial, String tipoOrganizacion, String rubro, List<RepresentanteDto> representantes) {
        static DonanteVista de(Donante d) {
            PersonaHumana h = d instanceof PersonaHumana x ? x : null;
            PersonaJuridica j = d instanceof PersonaJuridica x ? x : null;
            return new DonanteVista(d.id(), d.tipo().name(), d.nombreVisible(), d.email(), d.contacto().telefono(),
                    d.contacto().whatsapp(), d.contacto().preferido().name(), d.documento().tipo().name(),
                    d.documento().numero(), DireccionDto.de(d.direccion()), d.activo(), d.fechaAlta(), d.ultimaInteraccion(),
                    h == null ? null : h.nombre(), h == null ? null : h.apellido(), h == null ? null : h.edad(),
                    h == null || h.genero() == null ? null : h.genero().name(),
                    j == null ? null : j.razonSocial(), j == null || j.tipoOrganizacion() == null ? null : j.tipoOrganizacion().name(),
                    j == null ? null : j.rubro(),
                    j == null ? List.of() : j.representantes().stream().map(r -> new RepresentanteDto(r.nombre(), r.email())).toList());
        }
    }

    // ------------------------------------------------------------------ alta por la administración (sin clave)

    @PostMapping("/api/donantes/humanas")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DonanteVista registrarHumana(@Valid @RequestBody PersonaHumanaRequest req) {
        return DonanteVista.de(registrarHumana.ejecutar(req.aComando(null)));
    }

    @PostMapping("/api/donantes/juridicas")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DonanteVista registrarJuridica(@Valid @RequestBody PersonaJuridicaRequest req) {
        return DonanteVista.de(registrarJuridica.ejecutar(req.aComando(null)));
    }

    // ------------------------------------------------------------------ autorregistro (con clave)

    @PostMapping("/api/registro/donantes/humanas")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DonanteVista autorregistroHumana(@Valid @RequestBody PersonaHumanaRequest req) {
        exigirClave(req.clave());
        return DonanteVista.de(registrarHumana.ejecutar(req.aComando(req.clave())));
    }

    @PostMapping("/api/registro/donantes/juridicas")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DonanteVista autorregistroJuridica(@Valid @RequestBody PersonaJuridicaRequest req) {
        exigirClave(req.clave());
        return DonanteVista.de(registrarJuridica.ejecutar(req.aComando(req.clave())));
    }

    private static void exigirClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria para registrarse");
        }
    }

    // ------------------------------------------------------------------ consultas y gestión

    @GetMapping("/api/donantes")
    PaginaDto<DonanteVista> listar(@RequestParam(required = false) String texto,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   @RequestParam(defaultValue = "20") int tamanio) {
        return PaginaDto.de(consultas.listar(texto, pagina, tamanio).mapear(DonanteVista::de));
    }

    @GetMapping("/api/donantes/{id}")
    DonanteVista detalle(@PathVariable UUID id, Authentication auth) {
        return DonanteVista.de(consultas.detalle(id, Solicitantes.de(auth)));
    }

    @PutMapping("/api/donantes/{id}/contacto")
    @Transactional
    DonanteVista actualizarContacto(@PathVariable UUID id, @Valid @RequestBody ActualizarContactoRequest req,
                                    Authentication auth) {
        return DonanteVista.de(actualizarContacto.ejecutar(id, req.contacto().aDominio(),
                req.direccion() == null ? null : req.direccion().aDominio(), Solicitantes.de(auth)));
    }

    @DeleteMapping("/api/donantes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void darDeBaja(@PathVariable UUID id) {
        darDeBaja.ejecutar(id);
    }

    @PostMapping("/api/donantes/{id}/interacciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void interaccion(@PathVariable UUID id, Authentication auth) {
        Solicitantes.de(auth).exigirAdminODonante(id);
        registrarInteraccion.ejecutar(id);
    }
}
