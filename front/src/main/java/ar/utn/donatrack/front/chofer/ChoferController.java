package ar.utn.donatrack.front.chofer;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ErrorApi;
import ar.utn.donatrack.front.web.Avisos;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

/**
 * Vista móvil del chofer: emula la "aplicación móvil" (alternativa elegida para el monitoreo):
 * inicia la ruta y reporta su geolocalización mientras la ruta está activa.
 */
@Controller
@RequestMapping("/chofer")
class ChoferController {

    private final Apis apis;

    ChoferController(Apis apis) {
        this.apis = apis;
    }

    @GetMapping
    String rutas(Model model) {
        model.addAttribute("rutas", apis.logistica().lista("/api/chofer/rutas"));
        return "chofer/rutas";
    }

    @GetMapping("/rutas/{id}")
    String ruta(@PathVariable UUID id, Model model) {
        model.addAttribute("ruta", apis.logistica().get("/api/rutas/{id}", id));
        model.addAttribute("entregasPorId", apis.logistica().lista("/api/rutas/{id}/entregas", id).stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.get("id"), e -> e)));
        return "chofer/ruta";
    }

    @PostMapping("/rutas/{id}/inicio")
    String iniciar(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            apis.logistica().post("/api/rutas/{id}/inicio", null, id);
            Avisos.exito(ra, "¡Ruta iniciada! Avisamos a las entidades y donantes. Mantené esta pantalla abierta para compartir tu ubicación.");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/chofer/rutas/" + id;
    }

    @PostMapping("/entregas/{id}/no-recibida")
    String noRecibida(@PathVariable UUID id, @RequestParam UUID rutaId, @RequestParam String motivo, RedirectAttributes ra) {
        try {
            apis.logistica().post("/api/entregas/{id}/no-recibida", Map.of("motivo", motivo), id);
            Avisos.exito(ra, "Registramos la entrega como no recibida");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/chofer/rutas/" + rutaId;
    }
}
