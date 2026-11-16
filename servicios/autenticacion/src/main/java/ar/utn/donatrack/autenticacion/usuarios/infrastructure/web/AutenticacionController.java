package ar.utn.donatrack.autenticacion.usuarios.infrastructure.web;

import ar.utn.donatrack.autenticacion.usuarios.application.ActivarCuenta;
import ar.utn.donatrack.autenticacion.usuarios.application.CredencialesInvalidasException;
import ar.utn.donatrack.autenticacion.usuarios.application.EmitirTokenServicio;
import ar.utn.donatrack.autenticacion.usuarios.application.IniciarSesion;
import ar.utn.donatrack.autenticacion.usuarios.application.Sesion;
import ar.utn.donatrack.autenticacion.usuarios.application.TokenEmitido;
import ar.utn.donatrack.autenticacion.usuarios.infrastructure.seguridad.ClavesRsa;
import com.nimbusds.jose.jwk.JWKSet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class AutenticacionController {

    private final IniciarSesion iniciarSesion;
    private final ActivarCuenta activarCuenta;
    private final EmitirTokenServicio emitirTokenServicio;
    private final ClavesRsa claves;

    AutenticacionController(IniciarSesion iniciarSesion, ActivarCuenta activarCuenta,
                            EmitirTokenServicio emitirTokenServicio, ClavesRsa claves) {
        this.iniciarSesion = iniciarSesion;
        this.activarCuenta = activarCuenta;
        this.emitirTokenServicio = emitirTokenServicio;
        this.claves = claves;
    }

    record LoginRequest(@NotBlank String email, @NotBlank String clave) {
    }

    record ActivacionRequest(@NotBlank String token, @NotBlank String clave) {
    }

    record TokenServicioRequest(@NotBlank String clientId, @NotBlank String clientSecret) {
    }

    @PostMapping("/api/auth/login")
    Sesion login(@Valid @RequestBody LoginRequest req) {
        return iniciarSesion.ejecutar(req.email(), req.clave());
    }

    @PostMapping("/api/auth/activacion")
    Map<String, String> activar(@Valid @RequestBody ActivacionRequest req) {
        return Map.of("email", activarCuenta.ejecutar(req.token(), req.clave()));
    }

    @PostMapping("/api/auth/token-servicio")
    TokenEmitido tokenServicio(@Valid @RequestBody TokenServicioRequest req) {
        return emitirTokenServicio.ejecutar(req.clientId(), req.clientSecret());
    }

    /** Clave pública en formato JWK Set: los demás servicios validan firmas con ella. */
    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return new JWKSet(claves.jwk().toPublicJWK()).toJSONObject();
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credencialesInvalidas(CredencialesInvalidasException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        p.setTitle("No autenticado");
        return p;
    }
}
