package ar.utn.donatrack.autenticacion.usuarios.application;

import ar.utn.donatrack.autenticacion.usuarios.domain.PoliticaClaves;
import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.UsuarioRepositorio;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

import java.time.Clock;

public class ActivarCuenta {

    private final UsuarioRepositorio usuarios;
    private final HasheadorClaves hasheador;
    private final Clock reloj;

    public ActivarCuenta(UsuarioRepositorio usuarios, HasheadorClaves hasheador, Clock reloj) {
        this.usuarios = usuarios;
        this.hasheador = hasheador;
        this.reloj = reloj;
    }

    public String ejecutar(String token, String nuevaClave) {
        PoliticaClaves.validar(nuevaClave);
        String hash = TokensActivacion.hash(token == null ? "" : token);
        Usuario usuario = usuarios.buscarPorHashActivacion(hash)
                .orElseThrow(() -> new ReglaDeNegocioException("El enlace de activación no es válido o ya fue utilizado"));
        usuario.activar(hash, hasheador.hashear(nuevaClave), reloj.instant());
        usuarios.guardar(usuario);
        return usuario.email();
    }
}
