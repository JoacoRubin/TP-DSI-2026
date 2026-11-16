package ar.utn.donatrack.autenticacion.usuarios.infrastructure.persistencia;

import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.UsuarioRepositorio;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
class RepositorioUsuariosJpa implements UsuarioRepositorio {

    private final UsuarioJpaRepository jpa;

    RepositorioUsuariosJpa(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpa.findByEmail(email).map(UsuarioJpa::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorHashActivacion(String hash) {
        return jpa.findByTokenActivacionHash(hash).map(UsuarioJpa::aDominio);
    }

    @Override
    public List<Usuario> buscarPorEmails(Collection<String> emails) {
        return jpa.findByEmailIn(emails).stream().map(UsuarioJpa::aDominio).toList();
    }

    @Override
    public boolean existeEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public void guardar(Usuario usuario) {
        jpa.save(UsuarioJpa.desde(usuario));
    }

    @Override
    public void guardarTodos(Collection<Usuario> usuarios) {
        jpa.saveAll(usuarios.stream().map(UsuarioJpa::desde).toList());
    }
}
