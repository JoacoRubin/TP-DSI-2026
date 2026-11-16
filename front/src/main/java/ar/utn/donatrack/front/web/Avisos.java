package ar.utn.donatrack.front.web;

import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Notificaciones no intrusivas (toasts) que la vista muestra tras cada acción.
 * Se modelan como Map para que la vista acceda con {@code ${aviso.tipo}} sin depender de getters.
 */
public final class Avisos {

    private Avisos() {
    }

    private static Map<String, String> aviso(String tipo, String mensaje) {
        return Map.of("tipo", tipo, "mensaje", mensaje == null ? "" : mensaje);
    }

    public static void exito(RedirectAttributes ra, String mensaje) {
        ra.addFlashAttribute("aviso", aviso("exito", mensaje));
    }

    public static void error(RedirectAttributes ra, String mensaje) {
        ra.addFlashAttribute("aviso", aviso("error", mensaje));
    }

    public static void error(Model model, String mensaje) {
        model.addAttribute("aviso", aviso("error", mensaje));
    }
}
