package ar.utn.donatrack.front.web;

import ar.utn.donatrack.front.sesion.UsuarioSesion;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Sesion {

    private Sesion() {
    }

    public static UsuarioSesion actual() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (UsuarioSesion) p;
    }

    public static String referencia() {
        return actual().referencia();
    }
}
