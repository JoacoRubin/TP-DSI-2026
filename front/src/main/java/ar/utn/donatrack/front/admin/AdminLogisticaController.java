package ar.utn.donatrack.front.admin;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ErrorApi;
import ar.utn.donatrack.front.api.ServicioNoDisponible;
import ar.utn.donatrack.front.web.Avisos;
import ar.utn.donatrack.front.web.Formularios;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Flota (req. 18), planificación de rutas, entregas fallidas y monitoreo en tiempo real. */
@Controller
@RequestMapping("/admin")
class AdminLogisticaController {

    private final Apis apis;

    AdminLogisticaController(Apis apis) {
        this.apis = apis;
    }

    @GetMapping("/logistica")
    String logistica(@RequestParam(defaultValue = "PENDIENTE") String estado, Model model) {
        model.addAttribute("estado", estado);
        model.addAttribute("entregas", apis.logistica().lista("/api/entregas?estado={e}", estado));
        model.addAttribute("solicitudes", apis.logistica().lista("/api/planificaciones"));
        model.addAttribute("rutas", apis.logistica().lista("/api/rutas"));
        return "admin/logistica";
    }

    @PostMapping("/logistica/planificacion")
    String planificar(RedirectAttributes ra) {
        try {
            Map<String, Object> r = apis.logistica().post("/api/planificaciones", null);
            Object lotes = r.get("lotesEnviados");
            Avisos.exito(ra, "0".equals(String.valueOf(lotes)) ? "No hay entregas pendientes para planificar"
                    : ("1".equals(String.valueOf(lotes)) ? "Se envió 1 lote" : "Se enviaron " + lotes + " lotes")
                      + " al planificador. Las rutas aparecerán en unos segundos.");
        } catch (ErrorApi | ServicioNoDisponible e) {
            Avisos.error(ra, e instanceof ErrorApi ea ? ea.mensajeCompleto() : e.getMessage());
        }
        return "redirect:/admin/logistica?estado=EN_PLANIFICACION";
    }

    @PostMapping("/logistica/entregas/{id}/reprogramacion")
    String reprogramar(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            apis.logistica().post("/api/entregas/{id}/reprogramacion", null, id);
            Avisos.exito(ra, "La entrega volvió a pendiente y se incluirá en la próxima planificación");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/admin/logistica?estado=NO_RECIBIDA";
    }

    @GetMapping("/monitoreo")
    String monitoreo() {
        return "admin/monitoreo";
    }

    // ------------------------------------------------------------------ camiones

    @GetMapping("/camiones")
    String camiones(Model model) {
        model.addAttribute("camiones", apis.logistica().lista("/api/camiones"));
        return "admin/camiones";
    }

    private static Map<String, Object> camion(Map<String, String> f) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("patente", Formularios.texto(f, "patente"));
        c.put("volumenM3", Formularios.decimal(f, "volumenM3"));
        c.put("alturaM", Formularios.decimal(f, "alturaM"));
        c.put("cargaKg", Formularios.decimal(f, "cargaKg"));
        c.put("activo", f.containsKey("activo"));
        return c;
    }

    @PostMapping("/camiones")
    String registrarCamion(@RequestParam Map<String, String> form, RedirectAttributes ra) {
        try {
            apis.logistica().post("/api/camiones", camion(form));
            Avisos.exito(ra, "Camión registrado");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/admin/camiones";
    }

    @PostMapping("/camiones/{id}")
    String actualizarCamion(@PathVariable UUID id, @RequestParam Map<String, String> form, RedirectAttributes ra) {
        try {
            apis.logistica().put("/api/camiones/{id}", camion(form), id);
            Avisos.exito(ra, "Camión actualizado");
        } catch (ErrorApi e) {
            Avisos.error(ra, e.mensajeCompleto());
        }
        return "redirect:/admin/camiones";
    }
}
