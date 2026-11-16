package ar.utn.donatrack.autenticacion.usuarios.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia: lo declara el dominio, lo implementa la infraestructura. */
public interface UsuarioRepositorio {

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorHashActivacion(String hash);

    List<Usuario> buscarPorEmails(Collection<String> emails);

    boolean existeEmail(String email);

    void guardar(Usuario usuario);

    void guardarTodos(Collection<Usuario> usuarios);
}
