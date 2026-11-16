package ar.utn.donatrack.front.comun;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ErrorApi;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bandeja de notificaciones (req. 8 y 13) y endpoints JSON del propio front para las vistas
 * con actualización periódica (seguimiento, monitoreo, progreso de importación, GPS del chofer).
 * El navegador sólo habla con el front, que agrega el token del usuario.
 */
@Controller
class ComunController {

    private final Apis apis;

    ComunController(Apis apis) {
        this.apis = apis;
    }

    @GetMapping("/notificaciones")
    String notificaciones(Model model) {
        model.addAttribute("notificaciones", apis.notificaciones().lista("/api/notificaciones/mias"));
        return "comun/notificaciones";
    }

    @PostMapping("/notificaciones/{id}/leida")
    String leida(@PathVariable UUID id, RedirectAttributes ra) {
        apis.notificaciones().patch("/api/notificaciones/{id}/leida", Map.of(), id);
        return "redirect:/notificaciones";
    }

    @GetMapping("/front-api/notificaciones/no-leidas")
    @ResponseBody
    Map<String, Object> noLeidas() {
        try {
            long n = apis.notificaciones().lista("/api/notificaciones/mias").stream()
                    .filter(x -> !Boolean.TRUE.equals(x.get("leida"))).count();
            return Map.of("cantidad", n);
        } catch (RuntimeException e) {
            return Map.of("cantidad", 0);
        }
    }

    @GetMapping("/front-api/seguimiento")
    @ResponseBody
    List<Map<String, Object>> seguimiento() {
        return apis.logistica().lista("/api/seguimiento");
    }

    @GetMapping("/front-api/monitoreo")
    @ResponseBody
    List<Map<String, Object>> monitoreo() {
        return apis.logistica().lista("/api/monitoreo");
    }

    @GetMapping("/front-api/importaciones/{id}")
    @ResponseBody
    Map<String, Object> importacion(@PathVariable UUID id) {
        return apis.donaciones().get("/api/importaciones/{id}", id);
    }

    /** Reporte de geolocalización de la "app" del chofer. */
    @PostMapping("/front-api/chofer/rutas/{id}/posiciones")
    @ResponseBody
    ResponseEntity<Map<String, Object>> posicion(@PathVariable UUID id, @RequestBody Map<String, Object> posicion) {
        try {
            return ResponseEntity.ok(apis.logistica().post("/api/rutas/{id}/posiciones", posicion, id));
        } catch (ErrorApi e) {
            return ResponseEntity.status(e.estado()).body(Map.of("error", e.getMessage()));
        }
    }
}
