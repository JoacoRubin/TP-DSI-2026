package ar.utn.donatrack.autenticacion.usuarios.infrastructure.web;

import ar.utn.donatrack.autenticacion.usuarios.application.NuevoUsuario;
import ar.utn.donatrack.autenticacion.usuarios.application.RegistrarUsuario;
import ar.utn.donatrack.autenticacion.usuarios.application.RegistrarUsuariosEnLote;
import ar.utn.donatrack.autenticacion.usuarios.application.ResultadoAlta;
import ar.utn.donatrack.autenticacion.usuarios.domain.PoliticaAltaCuentas;
import ar.utn.donatrack.autenticacion.usuarios.domain.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Alta de cuentas: sólo la invocan otros servicios (token SERVICIO) o una persona administradora. */
@RestController
@RequestMapping("/api/usuarios")
class UsuariosController {

    private final RegistrarUsuario registrarUsuario;
    private final RegistrarUsuariosEnLote registrarEnLote;

    UsuariosController(RegistrarUsuario registrarUsuario, RegistrarUsuariosEnLote registrarEnLote) {
        this.registrarUsuario = registrarUsuario;
        this.registrarEnLote = registrarEnLote;
    }

    record NuevoUsuarioRequest(@NotBlank @Email String email, @NotBlank String nombre, @NotNull Rol rol,
                               String perfil, String referencia, String clave) {
        NuevoUsuario aComando() {
            return new NuevoUsuario(email, nombre, rol, perfil, referencia, clave);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    ResultadoAlta registrar(@Valid @RequestBody NuevoUsuarioRequest req, Authentication auth) {
        PoliticaAltaCuentas.exigir(autoridades(auth), req.rol());
        return registrarUsuario.ejecutar(req.aComando());
    }

    @PostMapping("/lote")
    @Transactional
    List<ResultadoAlta> registrarLote(@RequestBody @Size(max = 1000) List<@Valid NuevoUsuarioRequest> lote,
                                      Authentication auth) {
        Set<String> autoridades = autoridades(auth);
        lote.forEach(r -> PoliticaAltaCuentas.exigir(autoridades, r.rol()));
        return registrarEnLote.ejecutar(lote.stream().map(NuevoUsuarioRequest::aComando).toList());
    }

    private static Set<String> autoridades(Authentication auth) {
        return auth == null ? Set.of() : auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
