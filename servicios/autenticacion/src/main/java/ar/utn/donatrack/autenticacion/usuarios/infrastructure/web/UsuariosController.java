package ar.utn.donatrack.autenticacion.usuarios.infrastructure.web;

import ar.utn.donatrack.autenticacion.usuarios.application.NuevoUsuario;
import ar.utn.donatrack.autenticacion.usuarios.application.RegistrarUsuario;
import ar.utn.donatrack.autenticacion.usuarios.application.RegistrarUsuariosEnLote;
import ar.utn.donatrack.autenticacion.usuarios.application.ResultadoAlta;
import ar.utn.donatrack.autenticacion.usuarios.domain.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    ResultadoAlta registrar(@Valid @RequestBody NuevoUsuarioRequest req) {
        return registrarUsuario.ejecutar(req.aComando());
    }

    @PostMapping("/lote")
    @Transactional
    List<ResultadoAlta> registrarLote(@RequestBody @Size(max = 1000) List<@Valid NuevoUsuarioRequest> lote) {
        return registrarEnLote.ejecutar(lote.stream().map(NuevoUsuarioRequest::aComando).toList());
    }
}
