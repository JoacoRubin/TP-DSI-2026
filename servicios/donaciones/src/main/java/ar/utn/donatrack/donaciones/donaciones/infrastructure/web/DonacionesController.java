package ar.utn.donatrack.donaciones.donaciones.infrastructure.web;

import ar.utn.donatrack.donaciones.compartido.infrastructure.AlmacenFotos;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.DtosComunes.PaginaDto;
import ar.utn.donatrack.donaciones.compartido.infrastructure.web.Solicitantes;
import ar.utn.donatrack.donaciones.donaciones.application.ActualizarDonacion;
import ar.utn.donatrack.donaciones.donaciones.application.ConsultasDonaciones;
import ar.utn.donatrack.donaciones.donaciones.application.DevolverADeposito;
import ar.utn.donatrack.donaciones.donaciones.application.EliminarDonacion;
import ar.utn.donatrack.donaciones.donaciones.application.MarcarDonacionVencida;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarCargaDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.BienDeclarado;
import ar.utn.donatrack.donaciones.donaciones.domain.CargaDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoUso;
import ar.utn.donatrack.donaciones.donaciones.domain.FiltroDonaciones;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class DonacionesController {

    private final RegistrarCargaDonacion registrarCarga;
    private final ActualizarDonacion actualizar;
    private final EliminarDonacion eliminar;
    private final MarcarDonacionVencida marcarVencida;
    private final DevolverADeposito devolverADeposito;
    private final ConsultasDonaciones consultas;
    private final AlmacenFotos fotos;

    DonacionesController(RegistrarCargaDonacion registrarCarga, ActualizarDonacion actualizar, EliminarDonacion eliminar,
                         MarcarDonacionVencida marcarVencida, DevolverADeposito devolverADeposito,
                         ConsultasDonaciones consultas, AlmacenFotos fotos) {
        this.registrarCarga = registrarCarga;
        this.actualizar = actualizar;
        this.eliminar = eliminar;
        this.marcarVencida = marcarVencida;
        this.devolverADeposito = devolverADeposito;
        this.consultas = consultas;
        this.fotos = fotos;
    }

    record BienRequest(@NotBlank String descripcion, @NotNull UUID subcategoriaId, @Positive int cantidad, String unidad,
                       LocalDate fechaVencimiento, EstadoUso estadoUso, String fotoUrl, Double pesoKg, Double volumenM3) {
        BienDeclarado aDominio() {
            return new BienDeclarado(descripcion, subcategoriaId, cantidad, unidad, fechaVencimiento, estadoUso, fotoUrl,
                    pesoKg, volumenM3);
        }
    }

    record CargaRequest(@NotNull UUID donanteId, String descripcionGeneral, @NotEmpty List<@Valid BienRequest> bienes) {
    }

    record CambioEstadoRequest(@NotNull EstadoDonacion estado, String motivo) {
    }

    record ActualizarRequest(String descripcionGeneral) {
    }

    record BienVista(String descripcion, int cantidad, String fotoUrl) {
    }

    record CambioVista(String desde, String hasta, String hastaEtiqueta, Instant fecha, String responsable, String motivo) {
    }

    record DonacionVista(UUID id, UUID cargaId, UUID donanteId, String descripcionGeneral, String descripcionCorta,
                         String categoria, UUID subcategoriaId, String subcategoria, String unidad, int cantidadTotal,
                         LocalDate fechaVencimiento, String estadoUso, double pesoKg, double volumenM3, String estado,
                         String estadoEtiqueta, UUID entidadId, UUID necesidadId, String patenteEntrega,
                         Instant entregadaEn, Instant registradaEn, List<BienVista> bienes, List<CambioVista> historial) {
        static DonacionVista de(Donacion d) {
            return new DonacionVista(d.id(), d.cargaId(), d.donanteId(), d.descripcionGeneral(), d.descripcionCorta(),
                    d.categoriaNombre(), d.subcategoriaId(), d.subcategoriaNombre(), d.unidad(), d.cantidadTotal(),
                    d.fechaVencimiento(), d.estadoUso() == null ? null : d.estadoUso().name(), d.pesoKg(), d.volumenM3(),
                    d.estado().name(), d.estado().etiqueta(), d.entidadId(), d.necesidadId(), d.patenteEntrega(),
                    d.entregadaEn(), d.registradaEn(),
                    d.bienes().stream().map(b -> new BienVista(b.descripcion(), b.cantidad(), b.fotoUrl())).toList(),
                    d.historial().stream().map(c -> new CambioVista(c.desde() == null ? null : c.desde().name(),
                            c.hasta().name(), c.hasta().etiqueta(), c.fecha(), c.responsable(), c.motivo())).toList());
        }
    }

    @PostMapping("/api/donaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    List<DonacionVista> registrar(@Valid @RequestBody CargaRequest req, Authentication auth) {
        CargaDonacion carga = new CargaDonacion(req.donanteId(), req.descripcionGeneral(),
                req.bienes().stream().map(BienRequest::aDominio).toList());
        return registrarCarga.ejecutar(carga, Solicitantes.de(auth)).stream().map(DonacionVista::de).toList();
    }

    @GetMapping("/api/donaciones")
    PaginaDto<DonacionVista> buscar(@RequestParam(required = false) UUID donanteId,
                                    @RequestParam(required = false) UUID entidadId,
                                    @RequestParam(required = false) EstadoDonacion estado,
                                    @RequestParam(required = false) String categoria,
                                    @RequestParam(required = false) UUID subcategoriaId,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    @RequestParam(defaultValue = "20") int tamanio,
                                    Authentication auth) {
        return PaginaDto.de(consultas.buscar(new FiltroDonaciones(donanteId, entidadId, estado, categoria, subcategoriaId),
                pagina, tamanio, Solicitantes.de(auth)).mapear(DonacionVista::de));
    }

    @GetMapping("/api/donaciones/{id}")
    DonacionVista detalle(@PathVariable UUID id, Authentication auth) {
        return DonacionVista.de(consultas.detalle(id, Solicitantes.de(auth)));
    }

    @PatchMapping("/api/donaciones/{id}")
    @Transactional
    DonacionVista actualizar(@PathVariable UUID id, @RequestBody ActualizarRequest req) {
        return DonacionVista.de(actualizar.ejecutar(id, req.descripcionGeneral()));
    }

    /** Cambios de estado manuales permitidos a la administración (el resto los producen los flujos). */
    @PatchMapping("/api/donaciones/{id}/estado")
    @Transactional
    DonacionVista cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambioEstadoRequest req, Authentication auth) {
        return DonacionVista.de(switch (req.estado()) {
            case VENCIDA -> marcarVencida.ejecutar(id, req.motivo(), Solicitantes.de(auth));
            case EN_DEPOSITO -> devolverADeposito.ejecutar(id, req.motivo(), Solicitantes.de(auth));
            default -> throw new IllegalArgumentException(
                    "El estado " + req.estado() + " se alcanza automáticamente a través del flujo de asignación y logística");
        });
    }

    @DeleteMapping("/api/donaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void eliminar(@PathVariable UUID id) {
        eliminar.ejecutar(id);
    }

    // ------------------------------------------------------------------ fotos de bienes

    @PostMapping("/api/fotos")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, String> subirFoto(@RequestPart("archivo") MultipartFile archivo) {
        return Map.of("url", "/api/fotos/" + fotos.guardar(archivo));
    }

    @GetMapping("/api/fotos/{nombre}")
    ResponseEntity<Resource> foto(@PathVariable String nombre) {
        Path p = fotos.resolver(nombre);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .contentType(MediaTypeFactory.getMediaType(nombre).orElse(org.springframework.http.MediaType.IMAGE_JPEG))
                .body(new FileSystemResource(p));
    }
}
