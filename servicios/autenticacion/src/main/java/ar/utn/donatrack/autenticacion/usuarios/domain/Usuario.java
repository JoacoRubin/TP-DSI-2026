package ar.utn.donatrack.autenticacion.usuarios.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Credencial de acceso a DonaTrack. No confundir con el Donante o la Entidad: el usuario
 * sólo sabe autenticarse y a qué {@code referencia} de negocio representa.
 */
public class Usuario {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UUID id;
    private final String email;
    private String nombre;
    private final Rol rol;
    private String perfil;
    private String referencia;
    private String claveHash;
    private EstadoUsuario estado;
    private String tokenActivacionHash;
    private Instant tokenActivacionExpira;
    private final Instant creadoEn;

    /** Constructor de reconstitución (usado por la persistencia). */
    public Usuario(UUID id, String email, String nombre, Rol rol, String perfil, String referencia, String claveHash,
                   EstadoUsuario estado, String tokenActivacionHash, Instant tokenActivacionExpira, Instant creadoEn) {
        this.id = Objects.requireNonNull(id);
        this.email = normalizarEmail(email);
        this.nombre = requerirTexto(nombre, "nombre");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.perfil = perfil;
        this.referencia = referencia;
        this.claveHash = claveHash;
        this.estado = Objects.requireNonNull(estado);
        this.tokenActivacionHash = tokenActivacionHash;
        this.tokenActivacionExpira = tokenActivacionExpira;
        this.creadoEn = creadoEn;
    }

    public static Usuario conClave(UUID id, String email, String nombre, Rol rol, String perfil, String referencia,
                                   String claveHash, Instant ahora) {
        return new Usuario(id, email, nombre, rol, perfil, referencia, Objects.requireNonNull(claveHash),
                EstadoUsuario.ACTIVO, null, null, ahora);
    }

    public static Usuario pendienteDeActivacion(UUID id, String email, String nombre, Rol rol, String perfil,
                                                String referencia, String tokenHash, Instant expira, Instant ahora) {
        return new Usuario(id, email, nombre, rol, perfil, referencia, null,
                EstadoUsuario.PENDIENTE_ACTIVACION, Objects.requireNonNull(tokenHash), expira, ahora);
    }

    public static String normalizarEmail(String email) {
        String normalizado = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL.matcher(normalizado).matches()) {
            throw new ReglaDeNegocioException("Email inválido: " + email);
        }
        return normalizado;
    }

    public void verificarHabilitadoParaIngresar() {
        switch (estado) {
            case ACTIVO -> { }
            case PENDIENTE_ACTIVACION -> throw new ReglaDeNegocioException(
                    "Tu cuenta todavía no está activa: revisá tu correo para activarla");
            case BLOQUEADO -> throw new ReglaDeNegocioException("La cuenta está bloqueada");
        }
    }

    public void activar(String tokenHashRecibido, String nuevaClaveHash, Instant ahora) {
        if (estado != EstadoUsuario.PENDIENTE_ACTIVACION || tokenActivacionHash == null
                || !tokenActivacionHash.equals(tokenHashRecibido)) {
            throw new ReglaDeNegocioException("El enlace de activación no es válido o ya fue utilizado");
        }
        if (tokenActivacionExpira != null && ahora.isAfter(tokenActivacionExpira)) {
            throw new ReglaDeNegocioException("El enlace de activación venció; pedí uno nuevo");
        }
        this.claveHash = Objects.requireNonNull(nuevaClaveHash);
        this.estado = EstadoUsuario.ACTIVO;
        this.tokenActivacionHash = null;
        this.tokenActivacionExpira = null;
    }

    public void actualizarDatos(String nombre, String perfil, String referencia) {
        this.nombre = requerirTexto(nombre, "nombre");
        if (perfil != null) {
            this.perfil = perfil;
        }
        if (referencia != null) {
            this.referencia = referencia;
        }
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDeNegocioException("El campo " + campo + " es obligatorio");
        }
        return valor.trim();
    }

    public UUID id() { return id; }
    public String email() { return email; }
    public String nombre() { return nombre; }
    public Rol rol() { return rol; }
    public String perfil() { return perfil; }
    public String referencia() { return referencia; }
    public String claveHash() { return claveHash; }
    public EstadoUsuario estado() { return estado; }
    public String tokenActivacionHash() { return tokenActivacionHash; }
    public Instant tokenActivacionExpira() { return tokenActivacionExpira; }
    public Instant creadoEn() { return creadoEn; }
}
