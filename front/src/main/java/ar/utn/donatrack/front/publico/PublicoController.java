package ar.utn.donatrack.front.publico;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ClienteApi;
import ar.utn.donatrack.front.api.ServicioNoDisponible;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Sección pública (sin login): landing, galería, mapa de entregas, información legal. */
@Controller
class PublicoController {

    private final Apis apis;

    PublicoController(Apis apis) {
        this.apis = apis;
    }

    private List<Map<String, Object>> entregasPublicas(int dias, int limite) {
        try {
            return apis.logistica().lista("/api/publico/entregas?dias={d}&limite={l}", dias, limite);
        } catch (ServicioNoDisponible | ar.utn.donatrack.front.api.ErrorApi e) {
            return List.of();
        }
    }

    /** Totales reales (agregación en Logística). Vacío si Logística no responde: la vista no inventa cifras. */
    private Map<String, Object> resumen(int dias) {
        try {
            Map<String, Object> r = apis.logistica().get("/api/publico/resumen?dias={d}", dias);
            return r == null ? Map.of() : r;
        } catch (ServicioNoDisponible | ar.utn.donatrack.front.api.ErrorApi e) {
            return Map.of();
        }
    }

    /** Más marcadores degradan el mapa en el navegador; se muestran las más recientes. */
    static final int MAXIMO_EN_MAPA = 500;

    @GetMapping("/")
    String inicio(Model model) {
        // Antes: se traían hasta 200 entregas y se contaban; con más de 200 en el mes la cifra mentía.
        model.addAttribute("destacadas", entregasPublicas(30, 6));
        Map<String, Object> mes = resumen(30);
        model.addAttribute("entregadasMes", mes.getOrDefault("entregadas", "–"));
        model.addAttribute("entidadesMes", mes.getOrDefault("entidades", "–"));
        try {
            model.addAttribute("podio", apis.incentivos().get("/api/publico/podio"));
        } catch (RuntimeException e) {
            model.addAttribute("podio", null);
        }
        return "publico/inicio";
    }

    @GetMapping("/galeria")
    String galeria(Model model) {
        model.addAttribute("entregas", entregasPublicas(365, 120).stream()
                .filter(e -> e.get("fotos") instanceof List<?> l && !l.isEmpty()).toList());
        return "publico/galeria";
    }

    @GetMapping("/mapa")
    String mapa(Model model) {
        List<Map<String, Object>> entregas = entregasPublicas(3650, MAXIMO_EN_MAPA);
        model.addAttribute("entregas", entregas);
        Object total = resumen(3650).get("entregadas");
        // "total" sólo si de verdad es el total; si no, se aclara que es un recorte.
        model.addAttribute("totalEntregas", total instanceof Number n ? n.longValue() : (long) entregas.size());
        return "publico/mapa";
    }

    @GetMapping("/legal")
    String legal() {
        return "publico/legal";
    }

    @GetMapping("/accesibilidad")
    String accesibilidad() {
        return "publico/accesibilidad";
    }

    @GetMapping("/error/403")
    String prohibido(Model model) {
        model.addAttribute("titulo", "Acceso denegado");
        model.addAttribute("mensaje", "Tu cuenta no tiene acceso a esta sección.");
        return "error";
    }

    /**
     * Proxy de imágenes: el navegador sólo conoce al front. Así los servicios no quedan expuestos
     * y la política de caché se controla en un único lugar.
     */
    @GetMapping("/fotos/{servicio}/{nombre:.+}")
    ResponseEntity<byte[]> foto(@PathVariable String servicio, @PathVariable String nombre) {
        ClienteApi api = switch (servicio) {
            case "logistica" -> apis.logistica();
            case "donaciones" -> apis.donaciones();
            default -> null;
        };
        if (api == null || !nombre.matches("[\\w.-]+")) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = api.bytes("/api/fotos/{n}", nombre);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .contentType(MediaTypeFactory.getMediaType(nombre).orElse(MediaType.IMAGE_JPEG))
                .body(bytes);
    }
}
