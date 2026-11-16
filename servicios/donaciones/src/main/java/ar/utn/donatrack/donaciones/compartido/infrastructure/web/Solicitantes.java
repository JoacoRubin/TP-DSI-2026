package ar.utn.donatrack.donaciones.compartido.infrastructure.web;

import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.soporte.seguridad.Claims;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/** Traduce el JWT (detalle de infraestructura) al Solicitante (concepto de aplicación). */
public final class Solicitantes {

    private Solicitantes() {
    }

    public static Solicitante de(Authentication auth) {
        if (auth == null) {
            return new Solicitante("anonimo", "ANONIMO", null);
        }
        String rol = Claims.jwt(auth).map(j -> j.getClaimAsString(Claims.ROL)).orElse("ANONIMO");
        UUID ref = null;
        try {
            ref = Claims.referenciaUuid(auth).orElse(null);
        } catch (IllegalArgumentException noEsUuid) {
            // choferes tienen la patente como referencia
        }
        return new Solicitante(Claims.usuario(auth), rol, ref);
    }
}
