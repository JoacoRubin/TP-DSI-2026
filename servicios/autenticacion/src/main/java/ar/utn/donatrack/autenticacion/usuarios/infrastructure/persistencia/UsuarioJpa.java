package ar.utn.donatrack.autenticacion.usuarios.infrastructure.persistencia;

import ar.utn.donatrack.autenticacion.usuarios.domain.EstadoUsuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.Rol;
import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Modelo de persistencia. El dominio ({@link Usuario}) no sabe que existe. */
@Entity
@Table(name = "usuarios", indexes = {
        @Index(name = "ux_usuarios_email", columnList = "email", unique = true),
        @Index(name = "ix_usuarios_token", columnList = "tokenActivacionHash")})
public class UsuarioJpa {

    @Id
    private UUID id;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false)
    private String nombre;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;
    private String perfil;
    private String referencia;
    private String claveHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoUsuario estado;
    @Column(length = 64)
    private String tokenActivacionHash;
    private Instant tokenActivacionExpira;
    private Instant creadoEn;

    protected UsuarioJpa() {
    }

    static UsuarioJpa desde(Usuario u) {
        UsuarioJpa j = new UsuarioJpa();
        j.id = u.id();
        j.email = u.email();
        j.nombre = u.nombre();
        j.rol = u.rol();
        j.perfil = u.perfil();
        j.referencia = u.referencia();
        j.claveHash = u.claveHash();
        j.estado = u.estado();
        j.tokenActivacionHash = u.tokenActivacionHash();
        j.tokenActivacionExpira = u.tokenActivacionExpira();
        j.creadoEn = u.creadoEn();
        return j;
    }

    Usuario aDominio() {
        return new Usuario(id, email, nombre, rol, perfil, referencia, claveHash, estado, tokenActivacionHash,
                tokenActivacionExpira, creadoEn);
    }
}
