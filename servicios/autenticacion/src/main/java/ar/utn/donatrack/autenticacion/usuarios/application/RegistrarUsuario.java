package ar.utn.donatrack.autenticacion.usuarios.application;

import ar.utn.donatrack.autenticacion.usuarios.domain.PoliticaClaves;
import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.UsuarioRepositorio;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class RegistrarUsuario {

    private final UsuarioRepositorio usuarios;
    private final HasheadorClaves hasheador;
    private final GeneradorSecretos secretos;
    private final Clock reloj;

    public RegistrarUsuario(UsuarioRepositorio usuarios, HasheadorClaves hasheador, GeneradorSecretos secretos, Clock reloj) {
        this.usuarios = usuarios;
        this.hasheador = hasheador;
        this.secretos = secretos;
        this.reloj = reloj;
    }

    public ResultadoAlta ejecutar(NuevoUsuario cmd) {
        String email = Usuario.normalizarEmail(cmd.email());
        if (usuarios.existeEmail(email)) {
            throw new ReglaDeNegocioException("Ya existe una cuenta registrada con el email " + email);
        }
        Instant ahora = reloj.instant();
        Usuario usuario;
        String token = null;
        if (cmd.clave() != null) {
            PoliticaClaves.validar(cmd.clave());
            usuario = Usuario.conClave(UUID.randomUUID(), email, cmd.nombre(), cmd.rol(), cmd.perfil(), cmd.referencia(),
                    hasheador.hashear(cmd.clave()), ahora);
        } else {
            token = secretos.nuevo();
            usuario = Usuario.pendienteDeActivacion(UUID.randomUUID(), email, cmd.nombre(), cmd.rol(), cmd.perfil(),
                    cmd.referencia(), TokensActivacion.hash(token), ahora.plus(TokensActivacion.VIGENCIA), ahora);
        }
        usuarios.guardar(usuario);
        return new ResultadoAlta(usuario.id(), usuario.email(), true, token);
    }
}
