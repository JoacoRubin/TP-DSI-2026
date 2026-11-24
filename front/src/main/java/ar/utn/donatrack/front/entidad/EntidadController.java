package ar.utn.donatrack.front.entidad;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ErrorApi;
import ar.utn.donatrack.front.api.ServicioNoDisponible;
import ar.utn.donatrack.front.web.Avisos;
import ar.utn.donatrack.front.web.Formularios;
import ar.utn.donatrack.front.web.Multipartes;
import ar.utn.donatrack.front.web.Sesion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/entidad")
class EntidadController {

    private final Apis apis;

    EntidadController(Apis apis) {
        this.apis = apis;
    }

    @GetMapping
    String inicio(Model model) {
        String id = Sesion.referencia();
        model.addAttribute("entidad", apis.donaciones().get("/api/entidades/{id}", id));
        model.addAttribute("necesidades", apis.donaciones().lista("/api/entidades/{id}/necesidades", id));
        model.addAttribute("entregas", apis.logistica().lista("/api/entregas"));
        return "entidad/inicio";
    }

    /** Requerimiento 10: registrar necesidades materiales concretas. */
    @GetMapping("/necesidades")
    String necesidades(Model model) {
        String id = Sesion.referencia();
        model.addAttribute("necesidades", apis.donaciones().lista("/api/entidades/{id}/necesidades", id));
        model.addAttribute("categorias", apis.donaciones().lista("/api/categorias"));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", Map.of());
        }
        return "entidad/necesidades";
    }

    @PostMapping("/necesidades")
    String registrarNecesidad(@RequestParam Map<String, String> form, RedirectAttributes ra) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipo", Formularios.texto(form, "tipo"));
        cuerpo.put("subcategoriaId", Formularios.texto(form, "subcategoriaId"));
        cuerpo.put("descripcion", Formularios.texto(form, "descripcion"));
        cuerpo.put("cantidad", Formularios.entero(form, "cantidad"));
        cuerpo.put("unidad", Formularios.texto(form, "unidad"));
        cuerpo.put("periodicidad", Formularios.texto(form, "periodicidad"));
        try {
            apis.donaciones().post("/api/entidades/{id}/necesidades", cuerpo, Sesion.referencia());
            Avisos.exito(ra, "Necesidad publicada. Ya participa del proceso de asignación.");
        } catch (ErrorApi | ServicioNoDisponible e) {
            Avisos.error(ra, e instanceof ErrorApi ea ? ea.mensajeCompleto() : e.getMessage());
            ra.addFlashAttribute("form", form);
        }
        return "redirect:/entidad/necesidades";
    }

    @PostMapping("/necesidades/{id}/baja")
    String bajaNecesidad(@PathVariable UUID id, RedirectAttributes ra) {
        apis.donaciones().delete("/api/necesidades/{id}", id);
        Avisos.exito(ra, "La necesidad fue dada de baja");
        return "redirect:/entidad/necesidades";
    }

    /**
     * Requerimientos 11 y 12: estado de las donaciones asignadas + confirmación de recepción. Se
     * combinan dos fuentes: el estado de la donación (Donaciones) y la entrega física (Logística).
     */
    static final int DONACIONES_POR_PAGINA = 20;

    @GetMapping("/donaciones")
    String donaciones(@RequestParam(defaultValue = "0") int pagina, Model model) {
        Map<String, Object> resultado = apis.donaciones()
                .get("/api/donaciones?pagina=" + Math.max(0, pagina) + "&tamanio=" + DONACIONES_POR_PAGINA);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> donaciones = (List<Map<String, Object>>) resultado.get("elementos");
        // Sólo las entregas de ESTA página (antes: todas las de la entidad, con tope de 500).
        // Si hay varias por donación (cancelada + nueva), Logística devuelve la más nueva primero.
        Map<Object, Map<String, Object>> entregasPorDonacion = donaciones.isEmpty() ? Map.of()
                : apis.logistica().lista("/api/entregas?donacionIds=" + donaciones.stream()
                        .map(d -> String.valueOf(d.get("id"))).collect(Collectors.joining(","))).stream()
                .collect(Collectors.toMap(e -> e.get("donacionId"), Function.identity(), (masNueva, anterior) -> masNueva));
        model.addAttribute("resultado", resultado);
        model.addAttribute("donaciones", donaciones);
        model.addAttribute("entregas", entregasPorDonacion);
        return "entidad/donaciones";
    }

    @PostMapping("/entregas/{id}/recepcion")
    String confirmar(@PathVariable UUID id, @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos,
                     RedirectAttributes ra) {
        try {
            apis.logistica().multipart("/api/entregas/{id}/recepcion", Multipartes.de("fotos", fotos), id);
            Avisos.exito(ra, "¡Gracias! Confirmamos la recepción y avisamos a la persona donante.");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/entidad/donaciones";
    }

    @PostMapping("/entregas/{id}/fotos")
    String fotos(@PathVariable UUID id, @RequestParam("fotos") List<MultipartFile> fotos, RedirectAttributes ra) {
        try {
            apis.logistica().multipart("/api/entregas/{id}/fotos", Multipartes.de("fotos", fotos), id);
            Avisos.exito(ra, "Fotos agregadas");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/entidad/donaciones";
    }

    @PostMapping("/entregas/{id}/no-recibida")
    String noRecibida(@PathVariable UUID id, @RequestParam String motivo, RedirectAttributes ra) {
        try {
            apis.logistica().post("/api/entregas/{id}/no-recibida", Map.of("motivo", motivo), id);
            Avisos.exito(ra, "Registramos que no recibieron la entrega. La administración revisará el caso.");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/entidad/donaciones";
    }

    /** Requerimiento 14: seguimiento en el mapa. */
    @GetMapping("/seguimiento")
    String seguimiento() {
        return "comun/seguimiento";
    }
}
