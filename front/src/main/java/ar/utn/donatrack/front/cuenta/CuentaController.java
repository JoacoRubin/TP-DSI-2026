package ar.utn.donatrack.front.cuenta;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ErrorApi;
import ar.utn.donatrack.front.api.ServicioNoDisponible;
import ar.utn.donatrack.front.sesion.UsuarioSesion;
import ar.utn.donatrack.front.web.Avisos;
import ar.utn.donatrack.front.web.Formularios;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.utn.donatrack.front.web.Formularios.texto;

/** Inicio de sesión, registro de donantes y entidades, activación de cuentas. */
@Controller
class CuentaController {

    private final Apis apis;

    CuentaController(Apis apis) {
        this.apis = apis;
    }

    @GetMapping("/login")
    String login(HttpSession session, Model model, @RequestParam(required = false) String error) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioSesion) {
            return "redirect:/";
        }
        if (error != null) {
            Object ex = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            model.addAttribute("errorLogin", ex instanceof AuthenticationException ae ? ae.getMessage() : "No pudimos iniciar sesión");
        }
        return "cuenta/login";
    }

    @GetMapping("/registro")
    String registro() {
        return "cuenta/registro";
    }

    @GetMapping("/registro/{tipo}")
    String formulario(@PathVariable String tipo, Model model) {
        model.addAttribute("form", Map.of());
        return vista(tipo);
    }

    private static String vista(String tipo) {
        return switch (tipo) {
            case "donante-humano" -> "cuenta/registro-humano";
            case "donante-juridico" -> "cuenta/registro-juridico";
            case "entidad" -> "cuenta/registro-entidad";
            default -> "redirect:/registro";
        };
    }

    @PostMapping("/registro/{tipo}")
    String registrar(@PathVariable String tipo, @RequestParam Map<String, String> form, Model model, RedirectAttributes ra) {
        if (!java.util.Objects.equals(form.get("clave"), form.get("confirmacion"))) {
            Avisos.error(model, "Las contraseñas no coinciden");
            model.addAttribute("form", form);
            return vista(tipo);
        }
        if (form.get("terminos") == null) {
            Avisos.error(model, "Para registrarte tenés que aceptar los términos y la política de privacidad");
            model.addAttribute("form", form);
            return vista(tipo);
        }
        try {
            switch (tipo) {
                case "donante-humano" -> {
                    Map<String, Object> cuerpo = personaHumana(form, texto(form, "clave"));
                    // Consentimiento propio y separado de los términos (Ley 25.326): casilla sin marcar = no.
                    cuerpo.put("aceptaDifusionLogros", form.get("difusionLogros") != null);
                    apis.donaciones().post("/api/registro/donantes/humanas", cuerpo);
                }
                case "donante-juridico" -> apis.donaciones().post("/api/registro/donantes/juridicas", personaJuridica(form, texto(form, "clave")));
                case "entidad" -> apis.donaciones().post("/api/registro/entidades", entidad(form, texto(form, "clave")));
                default -> {
                    return "redirect:/registro";
                }
            }
        } catch (ErrorApi | ServicioNoDisponible e) {
            Avisos.error(model, e instanceof ErrorApi ea ? ea.mensajeCompleto() : e.getMessage());
            model.addAttribute("form", form);
            return vista(tipo);
        }
        Avisos.exito(ra, "¡Listo! Tu cuenta fue creada. Ya podés iniciar sesión.");
        return "redirect:/login";
    }

    // Construcción de cuerpos para la API (reutilizados por la administración)

    static Map<String, Object> personaHumana(Map<String, String> f, String clave) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("nombre", texto(f, "nombre"));
        b.put("apellido", texto(f, "apellido"));
        b.put("edad", Formularios.entero(f, "edad"));
        b.put("tipoDocumento", texto(f, "tipoDocumento"));
        b.put("numeroDocumento", texto(f, "numeroDocumento"));
        b.put("genero", texto(f, "genero"));
        b.put("direccion", Formularios.direccion(f));
        b.put("contacto", Formularios.contacto(f));
        b.put("clave", clave);
        return b;
    }

    static Map<String, Object> personaJuridica(Map<String, String> f, String clave) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("razonSocial", texto(f, "razonSocial"));
        b.put("tipo", texto(f, "tipo"));
        b.put("rubro", texto(f, "rubro"));
        b.put("cuit", texto(f, "cuit"));
        b.put("direccion", texto(f, "calle") == null ? null : Formularios.direccion(f));
        b.put("contacto", Formularios.contacto(f));
        b.put("representantes", texto(f, "representanteNombre") == null ? List.of()
                : List.of(Map.of("nombre", texto(f, "representanteNombre"), "email", String.valueOf(texto(f, "representanteEmail")))));
        b.put("clave", clave);
        return b;
    }

    static Map<String, Object> entidad(Map<String, String> f, String clave) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("razonSocial", texto(f, "razonSocial"));
        b.put("direccion", Formularios.direccion(f));
        b.put("telefono", texto(f, "telefono"));
        b.put("emails", java.util.Arrays.stream(String.valueOf(f.getOrDefault("emails", "")).split("[,;\\s]+"))
                .filter(s -> !s.isBlank()).toList());
        b.put("clave", clave);
        return b;
    }

    // ------------------------------------------------------------------ activación

    @GetMapping("/activar")
    String activar(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        return "cuenta/activar";
    }

    @PostMapping("/activar")
    String activarCuenta(@RequestParam String token, @RequestParam String clave, @RequestParam String confirmacion,
                         Model model, RedirectAttributes ra) {
        if (!clave.equals(confirmacion)) {
            Avisos.error(model, "Las contraseñas no coinciden");
            model.addAttribute("token", token);
            return "cuenta/activar";
        }
        try {
            apis.autenticacion().post("/api/auth/activacion", Map.of("token", token, "clave", clave));
        } catch (ErrorApi | ServicioNoDisponible e) {
            Avisos.error(model, e instanceof ErrorApi ea ? ea.mensajeCompleto() : e.getMessage());
            model.addAttribute("token", token);
            return "cuenta/activar";
        }
        Avisos.exito(ra, "¡Cuenta activada! Ingresá con tu email y tu nueva contraseña.");
        return "redirect:/login";
    }
}
