package ar.utn.donatrack.autenticacion.usuarios.infrastructure.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface UsuarioJpaRepository extends JpaRepository<UsuarioJpa, UUID> {

    Optional<UsuarioJpa> findByEmail(String email);

    Optional<UsuarioJpa> findByTokenActivacionHash(String hash);

    List<UsuarioJpa> findByEmailIn(Collection<String> emails);

    boolean existsByEmail(String email);
}
