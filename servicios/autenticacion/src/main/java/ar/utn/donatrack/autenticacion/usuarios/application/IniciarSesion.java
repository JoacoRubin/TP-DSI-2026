package ar.utn.donatrack.autenticacion.usuarios.application;

import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.UsuarioRepositorio;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

public class IniciarSesion {

    private final UsuarioRepositorio usuarios;
    private final HasheadorClaves hasheador;
    private final EmisorTokens emisor;

    public IniciarSesion(UsuarioRepositorio usuarios, HasheadorClaves hasheador, EmisorTokens emisor) {
        this.usuarios = usuarios;
        this.hasheador = hasheador;
        this.emisor = emisor;
    }

    public Sesion ejecutar(String email, String clave) {
        Usuario usuario;
        try {
            usuario = usuarios.buscarPorEmail(Usuario.normalizarEmail(email)).orElseThrow(CredencialesInvalidasException::new);
        } catch (ReglaDeNegocioException emailMalFormado) {
            throw new CredencialesInvalidasException();
        }
        usuario.verificarHabilitadoParaIngresar();
        if (clave == null || !hasheador.coincide(clave, usuario.claveHash())) {
            throw new CredencialesInvalidasException();
        }
        return new Sesion(emisor.paraUsuario(usuario), usuario.id(), usuario.email(), usuario.nombre(),
                usuario.rol().name(), usuario.perfil(), usuario.referencia());
    }
}
