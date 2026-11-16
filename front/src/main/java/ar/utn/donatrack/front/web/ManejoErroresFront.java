package ar.utn.donatrack.front.web;

import ar.utn.donatrack.front.api.ErrorApi;
import ar.utn.donatrack.front.api.ServicioNoDisponible;
import ar.utn.donatrack.front.sesion.UsuarioSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Errores de páginas GET: se muestra una página amigable. (Los POST de formularios atrapan
 * {@link ErrorApi} en el controlador para re-renderizar el formulario con los datos cargados.)
 */
@ControllerAdvice
class ManejoErroresFront {

    private static final Logger log = LoggerFactory.getLogger(ManejoErroresFront.class);

    @ModelAttribute("usuario")
    UsuarioSesion usuario() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof UsuarioSesion u ? u : null;
    }

    @ExceptionHandler(ErrorApi.class)
    String errorApi(ErrorApi e, HttpServletRequest req, HttpServletResponse res, Model model) {
        if (e.noAutenticado()) {
            new SecurityContextLogoutHandler().logout(req, res, SecurityContextHolder.getContext().getAuthentication());
            return "redirect:/login?expirada";
        }
        res.setStatus(e.estado() == 404 ? 404 : e.estado() == 403 ? 403 : 500);
        model.addAttribute("titulo", e.estado() == 404 ? "No encontrado" : e.estado() == 403 ? "Acceso denegado" : "Algo salió mal");
        model.addAttribute("mensaje", e.getMessage());
        return "error";
    }

    @ExceptionHandler(ServicioNoDisponible.class)
    String noDisponible(ServicioNoDisponible e, HttpServletResponse res, Model model) {
        log.warn(e.getMessage());
        res.setStatus(503);
        model.addAttribute("titulo", "Servicio no disponible");
        model.addAttribute("mensaje", e.getMessage());
        return "error";
    }
}
