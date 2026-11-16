package ar.utn.donatrack.front.donante;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.sesion.UsuarioSesion;
import ar.utn.donatrack.front.web.Avisos;
import ar.utn.donatrack.front.web.Sesion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/donante")
class DonanteController {

    private final Apis apis;

    DonanteController(Apis apis) {
        this.apis = apis;
    }

    @GetMapping
    String inicio(Model model) {
        UsuarioSesion u = Sesion.actual();
        model.addAttribute("recientes", apis.donaciones().get("/api/donaciones?tamanio=5").get("elementos"));
        if (u.esDonanteHumano()) {
            try {
                model.addAttribute("metricas", apis.incentivos().get("/api/donantes/{id}/metricas", u.referencia()));
            } catch (RuntimeException e) {
                model.addAttribute("metricas", null); // aún sin actividad registrada en Incentivos
            }
        }
        return "donante/inicio";
    }

    /** Requerimiento 5: filtrar por estado y por categoría o subcategoría. */
    @GetMapping("/donaciones")
    String donaciones(@RequestParam(required = false) String estado, @RequestParam(required = false) String categoria,
                      @RequestParam(required = false) UUID subcategoriaId, @RequestParam(defaultValue = "0") int pagina,
                      Model model) {
        String uri = UriComponentsBuilder.fromPath("/api/donaciones")
                .queryParamIfPresent("estado", java.util.Optional.ofNullable(blank(estado)))
                .queryParamIfPresent("categoria", java.util.Optional.ofNullable(blank(categoria)))
                .queryParamIfPresent("subcategoriaId", java.util.Optional.ofNullable(subcategoriaId))
                .queryParam("pagina", pagina).queryParam("tamanio", 10)
                .build().toUriString();
        model.addAttribute("resultado", apis.donaciones().get(uri));
        model.addAttribute("categorias", apis.donaciones().lista("/api/categorias"));
        model.addAttribute("filtro", Map.of("estado", estado == null ? "" : estado, "categoria", categoria == null ? "" : categoria,
                "subcategoriaId", subcategoriaId == null ? "" : subcategoriaId.toString()));
        return "donante/donaciones";
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    @GetMapping("/donaciones/{id}")
    String donacion(@PathVariable UUID id, Model model) {
        Map<String, Object> d = apis.donaciones().get("/api/donaciones/{id}", id);
        model.addAttribute("donacion", d);
        if (d.get("entidadId") != null) {
            model.addAttribute("entidad", apis.donaciones().get("/api/entidades/{id}", d.get("entidadId")));
        }
        return "donante/donacion";
    }

    /** Requerimiento 6: navegar por las entidades beneficiarias. */
    @GetMapping("/entidades")
    String entidades(@RequestParam(required = false) String texto, Model model) {
        model.addAttribute("entidades", apis.donaciones().lista("/api/entidades?texto={t}", texto == null ? "" : texto));
        model.addAttribute("texto", texto);
        return "donante/entidades";
    }

    @GetMapping("/entidades/{id}")
    String entidad(@PathVariable UUID id, Model model) {
        model.addAttribute("entidad", apis.donaciones().get("/api/entidades/{id}", id));
        model.addAttribute("necesidades", apis.donaciones().lista("/api/entidades/{id}/necesidades", id));
        return "donante/entidad";
    }

    /** Requerimiento 7: incentivos (sólo personas humanas; ver regla en la seguridad del front). */
    @GetMapping("/incentivos")
    String incentivos(Model model) {
        String id = Sesion.referencia();
        try {
            model.addAttribute("panel", apis.incentivos().get("/api/donantes/{id}/incentivos", id));
            model.addAttribute("metricas", apis.incentivos().get("/api/donantes/{id}/metricas", id));
        } catch (ar.utn.donatrack.front.api.ErrorApi e) {
            if (e.estado() != 404) {
                throw e;
            }
            model.addAttribute("panel", null);
        }
        return "donante/incentivos";
    }

    @PostMapping("/incentivos/preferencias")
    String preferencias(@RequestParam(defaultValue = "false") boolean insigniasVisibles, RedirectAttributes ra) {
        apis.incentivos().patch("/api/donantes/{id}/preferencias", Map.of("insigniasVisibles", insigniasVisibles), Sesion.referencia());
        Avisos.exito(ra, insigniasVisibles ? "Tus insignias ahora son visibles" : "Tus insignias ahora son privadas");
        return "redirect:/donante/incentivos";
    }

    /** Requerimiento 9: seguir en un mapa las entregas activas. */
    @GetMapping("/seguimiento")
    String seguimiento(Model model) {
        model.addAttribute("rutas", List.of());
        return "comun/seguimiento";
    }
}
