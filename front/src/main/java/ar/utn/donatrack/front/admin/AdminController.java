package ar.utn.donatrack.front.admin;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ErrorApi;
import ar.utn.donatrack.front.api.ServicioNoDisponible;
import ar.utn.donatrack.front.cuenta.Cuerpos;
import ar.utn.donatrack.front.web.Avisos;
import ar.utn.donatrack.front.web.Formularios;
import ar.utn.donatrack.front.web.Multipartes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Panel de administración: donantes, donaciones, asignaciones, entidades, rankings e importación. */
@Controller
@RequestMapping("/admin")
class AdminController {

    private final Apis apis;

    AdminController(Apis apis) {
        this.apis = apis;
    }

    private static String mensaje(RuntimeException e) {
        return e instanceof ErrorApi ea ? ea.mensajeCompleto() : e.getMessage();
    }

    @GetMapping
    String inicio(Model model) {
        model.addAttribute("pendientes", apis.donaciones().lista("/api/matchmaking/pendientes"));
        try {
            model.addAttribute("monitoreo", apis.logistica().lista("/api/monitoreo"));
            model.addAttribute("entregasPendientes", apis.logistica().lista("/api/entregas?estado=PENDIENTE"));
            model.addAttribute("noRecibidas", apis.logistica().lista("/api/entregas?estado=NO_RECIBIDA"));
        } catch (ServicioNoDisponible e) {
            Avisos.error(model, e.getMessage());
        }
        return "admin/inicio";
    }

    // ================================================================ donantes (req. 15)

    @GetMapping("/donantes")
    String donantes(@RequestParam(required = false) String texto, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("resultado", apis.donaciones().get("/api/donantes?texto={t}&pagina={p}&tamanio=15",
                texto == null ? "" : texto, pagina));
        model.addAttribute("texto", texto);
        return "admin/donantes";
    }

    @GetMapping("/donantes/nuevo")
    String nuevoDonante(@RequestParam(defaultValue = "humana") String tipo, Model model) {
        model.addAttribute("tipo", tipo);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", Map.of());
        }
        return "admin/donante-nuevo";
    }

    @PostMapping("/donantes/{tipo}")
    String registrarDonante(@PathVariable String tipo, @RequestParam Map<String, String> form, Model model, RedirectAttributes ra) {
        try {
            Map<String, Object> d = "juridica".equals(tipo)
                    ? apis.donaciones().post("/api/donantes/juridicas", Cuerpos.personaJuridica(form))
                    : apis.donaciones().post("/api/donantes/humanas", Cuerpos.personaHumana(form));
            Avisos.exito(ra, "Donante registrado. Le enviamos un correo de bienvenida para activar su cuenta.");
            return "redirect:/admin/donaciones/nueva?donanteId=" + d.get("id");
        } catch (ErrorApi | ServicioNoDisponible e) {
            Avisos.error(model, mensaje(e));
            model.addAttribute("form", form);
            model.addAttribute("tipo", tipo);
            return "admin/donante-nuevo";
        }
    }

    @PostMapping("/donantes/{id}/baja")
    String bajaDonante(@PathVariable UUID id, RedirectAttributes ra) {
        apis.donaciones().delete("/api/donantes/{id}", id);
        Avisos.exito(ra, "Donante dado de baja");
        return "redirect:/admin/donantes";
    }

    // ================================================================ donaciones (req. 15 y 16)

    @GetMapping("/donaciones")
    String donaciones(@RequestParam(required = false) String estado, @RequestParam(required = false) String categoria,
                      @RequestParam(defaultValue = "0") int pagina, Model model) {
        String uri = UriComponentsBuilder.fromPath("/api/donaciones")
                .queryParamIfPresent("estado", Optional.ofNullable(estado).filter(s -> !s.isBlank()))
                .queryParamIfPresent("categoria", Optional.ofNullable(categoria).filter(s -> !s.isBlank()))
                .queryParam("pagina", pagina).queryParam("tamanio", 15).build().toUriString();
        model.addAttribute("resultado", apis.donaciones().get(uri));
        model.addAttribute("categorias", apis.donaciones().lista("/api/categorias"));
        model.addAttribute("filtro", Map.of("estado", estado == null ? "" : estado, "categoria", categoria == null ? "" : categoria));
        return "admin/donaciones";
    }

    @GetMapping("/donaciones/{id}")
    String donacion(@PathVariable UUID id, Model model) {
        Map<String, Object> d = apis.donaciones().get("/api/donaciones/{id}", id);
        model.addAttribute("donacion", d);
        model.addAttribute("donante", apis.donaciones().get("/api/donantes/{id}", d.get("donanteId")));
        return "admin/donacion";
    }

    @GetMapping("/donaciones/nueva")
    String nuevaDonacion(@RequestParam(required = false) UUID donanteId, @RequestParam(required = false) String buscar, Model model) {
        if (donanteId != null) {
            model.addAttribute("donante", apis.donaciones().get("/api/donantes/{id}", donanteId));
        } else if (buscar != null && !buscar.isBlank()) {
            model.addAttribute("candidatos", apis.donaciones().get("/api/donantes?texto={t}&tamanio=10", buscar).get("elementos"));
        }
        model.addAttribute("buscar", buscar);
        model.addAttribute("categorias", apis.donaciones().lista("/api/categorias"));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", Map.of());
        }
        return "admin/donacion-nueva";
    }

    /** Carga única: todos los bienes en un formulario; el servicio segmenta por subcategoría. */
    @PostMapping("/donaciones")
    String registrarDonacion(@RequestParam Map<String, String> form, MultipartHttpServletRequest archivos,
                             RedirectAttributes ra) {
        List<Map<String, Object>> bienes = new ArrayList<>();
        List<Map<String, String>> filas = Formularios.filas(form, "bienes");
        try {
            for (int i = 0; i < filas.size(); i++) {
                Map<String, String> f = filas.get(i);
                if (Formularios.texto(f, "descripcion") == null) {
                    continue;
                }
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("descripcion", Formularios.texto(f, "descripcion"));
                b.put("subcategoriaId", Formularios.texto(f, "subcategoriaId"));
                b.put("cantidad", Formularios.entero(f, "cantidad"));
                b.put("unidad", Formularios.texto(f, "unidad"));
                b.put("fechaVencimiento", Formularios.texto(f, "fechaVencimiento"));
                b.put("estadoUso", Formularios.texto(f, "estadoUso"));
                b.put("pesoKg", Formularios.decimal(f, "pesoKg"));
                b.put("volumenM3", Formularios.decimal(f, "volumenM3"));
                MultipartFile foto = archivos.getFile("foto_" + f.getOrDefault("indice", ""));
                if (foto != null && !foto.isEmpty()) {
                    MultiValueMap<String, Object> partes = new org.springframework.util.LinkedMultiValueMap<>();
                    partes.add("archivo", Multipartes.entidad(foto));
                    b.put("fotoUrl", apis.donaciones().multipart("/api/fotos", partes).get("url"));
                }
                bienes.add(b);
            }
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("donanteId", Formularios.texto(form, "donanteId"));
            cuerpo.put("descripcionGeneral", Formularios.texto(form, "descripcionGeneral"));
            cuerpo.put("bienes", bienes);
            List<Map<String, Object>> resultado = apis.donaciones().postLista("/api/donaciones", cuerpo);
            Avisos.exito(ra, resultado.size() == 1 ? "Carga registrada: se generó 1 donación."
                    : "Carga registrada: se generaron " + resultado.size() + " donaciones (una por subcategoría).");
            return "redirect:/admin/donaciones";
        } catch (ErrorApi | ServicioNoDisponible e) {
            Avisos.error(ra, mensaje(e));
            ra.addFlashAttribute("form", form);
            return "redirect:/admin/donaciones/nueva?donanteId=" + form.getOrDefault("donanteId", "");
        }
    }

    @PostMapping("/donaciones/{id}/estado")
    String cambiarEstado(@PathVariable UUID id, @RequestParam String estado, @RequestParam(required = false) String motivo,
                         RedirectAttributes ra) {
        try {
            apis.donaciones().patch("/api/donaciones/{id}/estado", Map.of("estado", estado, "motivo", motivo == null ? "" : motivo), id);
            Avisos.exito(ra, "VENCIDA".equals(estado) ? "Donación marcada como vencida" : "La donación volvió al depósito");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/admin/donaciones/" + id;
    }

    // ================================================================ asignaciones (req. 17)

    @GetMapping("/asignaciones")
    String asignaciones(Model model) {
        model.addAttribute("pendientes", apis.donaciones().lista("/api/matchmaking/pendientes"));
        model.addAttribute("pesos", apis.donaciones().get("/api/matchmaking/pesos"));
        return "admin/asignaciones";
    }

    @PostMapping("/asignaciones/ejecucion")
    String ejecutarMatchmaking(RedirectAttributes ra) {
        apis.donaciones().post("/api/matchmaking/ejecuciones", null);
        Avisos.exito(ra, "Los algoritmos se están ejecutando en segundo plano. Actualizá en unos segundos.");
        return "redirect:/admin/asignaciones";
    }

    @GetMapping("/asignaciones/{id}")
    String asignacion(@PathVariable UUID id, Model model) {
        model.addAttribute("detalle", apis.donaciones().get("/api/matchmaking/donaciones/{id}", id));
        return "admin/asignacion";
    }

    @PostMapping("/asignaciones/{id}/evaluacion")
    String evaluar(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            apis.donaciones().post("/api/matchmaking/donaciones/{id}/evaluacion", null, id);
            Avisos.exito(ra, "Ranking recalculado");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/admin/asignaciones/" + id;
    }

    @PostMapping("/asignaciones/{id}/confirmacion")
    String confirmar(@PathVariable UUID id, @RequestParam UUID necesidadId, RedirectAttributes ra) {
        try {
            apis.donaciones().post("/api/donaciones/{id}/asignacion", Map.of("necesidadId", necesidadId), id);
            Avisos.exito(ra, "¡Asignación confirmada! Notificamos a la entidad y a la persona donante.");
            return "redirect:/admin/asignaciones";
        } catch (ErrorApi | ServicioNoDisponible e) {
            Avisos.error(ra, mensaje(e));
            return "redirect:/admin/asignaciones/" + id;
        }
    }

    @PostMapping("/asignaciones/pesos")
    String pesos(@RequestParam double textual, @RequestParam double volumen, @RequestParam double tipo, RedirectAttributes ra) {
        try {
            apis.donaciones().put("/api/matchmaking/pesos", Map.of("textual", textual, "volumen", volumen, "tipo", tipo));
            Avisos.exito(ra, "Pesos del algoritmo actualizados");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/admin/asignaciones";
    }

    // ================================================================ entidades

    @GetMapping("/entidades")
    String entidades(@RequestParam(required = false) String texto, Model model) {
        model.addAttribute("entidades", apis.donaciones().lista("/api/entidades?texto={t}", texto == null ? "" : texto));
        model.addAttribute("texto", texto);
        return "admin/entidades";
    }

    @GetMapping("/entidades/nueva")
    String nuevaEntidad(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", Map.of());
        }
        return "admin/entidad-nueva";
    }

    @PostMapping("/entidades")
    String registrarEntidad(@RequestParam Map<String, String> form, Model model, RedirectAttributes ra) {
        try {
            apis.donaciones().post("/api/entidades", Cuerpos.entidad(form));
            Avisos.exito(ra, "Entidad registrada. Enviamos el enlace de activación al correo indicado.");
            return "redirect:/admin/entidades";
        } catch (ErrorApi | ServicioNoDisponible e) {
            Avisos.error(model, mensaje(e));
            model.addAttribute("form", form);
            return "admin/entidad-nueva";
        }
    }

    // ================================================================ rankings (req. 19)

    @GetMapping("/rankings")
    String rankings(@RequestParam(required = false) String periodo, Model model) {
        List<Map<String, Object>> historial = apis.incentivos().lista("/api/rankings");
        model.addAttribute("historial", historial);
        model.addAttribute("actual", apis.incentivos().get("/api/rankings/actual"));
        if (periodo != null && !periodo.isBlank()) {
            model.addAttribute("seleccionado", apis.incentivos().get("/api/rankings/{p}", periodo));
        } else if (!historial.isEmpty()) {
            model.addAttribute("seleccionado", historial.get(0));
        }
        return "admin/rankings";
    }

    @PostMapping("/rankings/generacion")
    String generarRanking(@RequestParam String periodo, RedirectAttributes ra) {
        apis.incentivos().post("/api/rankings/{p}/generacion", null, periodo);
        Avisos.exito(ra, "Ranking de " + periodo + " generado");
        return "redirect:/admin/rankings?periodo=" + periodo;
    }

    // ================================================================ importación CSV (req. 20)

    @GetMapping("/importaciones")
    String importaciones(Model model) {
        model.addAttribute("importaciones", apis.donaciones().lista("/api/importaciones"));
        return "admin/importaciones";
    }

    @PostMapping("/importaciones")
    String importar(@RequestParam("archivo") MultipartFile archivo, RedirectAttributes ra) {
        if (archivo.isEmpty()) {
            Avisos.error(ra, "Elegí un archivo .csv");
            return "redirect:/admin/importaciones";
        }
        try {
            MultiValueMap<String, Object> partes = new org.springframework.util.LinkedMultiValueMap<>();
            partes.add("archivo", Multipartes.entidad(archivo));
            Map<String, Object> imp = apis.donaciones().multipart("/api/importaciones", partes);
            Avisos.exito(ra, "Importación iniciada. Podés seguir el progreso en esta pantalla.");
            return "redirect:/admin/importaciones/" + imp.get("id");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
            return "redirect:/admin/importaciones";
        }
    }

    @GetMapping("/importaciones/{id}")
    String importacion(@PathVariable UUID id, Model model) {
        model.addAttribute("importacion", apis.donaciones().get("/api/importaciones/{id}", id));
        return "admin/importacion";
    }
}
