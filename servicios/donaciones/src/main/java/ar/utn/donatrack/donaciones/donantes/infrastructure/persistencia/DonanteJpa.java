package ar.utn.donatrack.donaciones.donantes.infrastructure.persistencia;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.compartido.infrastructure.persistencia.DireccionJpa;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaJuridica;
import ar.utn.donatrack.donaciones.donantes.domain.Representante;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDonante;
import ar.utn.donatrack.donaciones.donantes.domain.TipoPersonaJuridica;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estrategia de herencia "single table" implementada a mano: una tabla con columna
 * discriminadora {@code tipo}. Es la opción más rápida para listar/buscar donantes de ambos
 * tipos sin JOINs ni UNIONs; el costo son columnas nulas según el tipo.
 * {@code nombreVisible} es una desnormalización para búsqueda y ordenamiento.
 */
@Entity
@Table(name = "donantes", indexes = {
        @Index(name = "ux_donantes_email", columnList = "email", unique = true),
        @Index(name = "ix_donantes_nombre", columnList = "nombreVisible"),
        @Index(name = "ix_donantes_interaccion", columnList = "activo,ultimaInteraccion")})
public class DonanteJpa {

    @Embeddable
    public static class RepresentanteJpa {
        private String nombre;
        private String email;

        protected RepresentanteJpa() {
        }

        RepresentanteJpa(String nombre, String email) {
            this.nombre = nombre;
            this.email = email;
        }
    }

    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoDonante tipo;
    @Column(nullable = false, length = 254)
    private String email;
    private String telefono;
    private String whatsapp;
    @Enumerated(EnumType.STRING)
    private MedioNotificacion medioPreferido;
    @Embedded
    private DireccionJpa direccion;
    private boolean activo;
    private Instant fechaAlta;
    private Instant ultimaInteraccion;
    private Instant ultimoAvisoInactividad;
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    @Column(nullable = false)
    private String nombreVisible;
    // Persona humana
    private String nombre;
    private String apellido;
    private Integer edad;
    @Enumerated(EnumType.STRING)
    private Genero genero;
    /** Boolean (no boolean): con ddl-auto=update la columna se agrega nullable y las filas previas quedan en NULL = no. */
    private Boolean aceptaDifusionLogros;
    // Persona jurídica
    private String razonSocial;
    @Enumerated(EnumType.STRING)
    private TipoPersonaJuridica tipoOrganizacion;
    private String rubro;
    @ElementCollection
    @CollectionTable(name = "donantes_representantes", joinColumns = @JoinColumn(name = "donante_id"))
    private List<RepresentanteJpa> representantes = new ArrayList<>();

    protected DonanteJpa() {
    }

    public UUID getId() {
        return id;
    }

    static DonanteJpa desde(Donante d) {
        DonanteJpa j = new DonanteJpa();
        j.id = d.id();
        j.tipo = d.tipo();
        j.email = d.email();
        j.telefono = d.contacto().telefono();
        j.whatsapp = d.contacto().whatsapp();
        j.medioPreferido = d.contacto().preferido();
        j.direccion = DireccionJpa.desde(d.direccion());
        j.activo = d.activo();
        j.fechaAlta = d.fechaAlta();
        j.ultimaInteraccion = d.ultimaInteraccion();
        j.ultimoAvisoInactividad = d.ultimoAvisoInactividad();
        j.tipoDocumento = d.documento().tipo();
        j.numeroDocumento = d.documento().numero();
        j.nombreVisible = d.nombreVisible();
        if (d instanceof PersonaHumana h) {
            j.nombre = h.nombre();
            j.apellido = h.apellido();
            j.edad = h.edad();
            j.genero = h.genero();
            j.aceptaDifusionLogros = h.aceptaDifusionLogros();
        } else if (d instanceof PersonaJuridica p) {
            j.razonSocial = p.razonSocial();
            j.tipoOrganizacion = p.tipoOrganizacion();
            j.rubro = p.rubro();
            j.representantes = new ArrayList<>(p.representantes().stream()
                    .map(r -> new RepresentanteJpa(r.nombre(), r.email())).toList());
        }
        return j;
    }

    Donante aDominio() {
        MediosContacto contacto = new MediosContacto(email, telefono, whatsapp, medioPreferido);
        Documento documento = new Documento(tipoDocumento, numeroDocumento);
        if (tipo == TipoDonante.HUMANA) {
            return new PersonaHumana(id, nombre, apellido, edad, documento, genero,
                    direccion == null ? null : direccion.aDominio(), contacto, activo, fechaAlta, ultimaInteraccion,
                    ultimoAvisoInactividad, Boolean.TRUE.equals(aceptaDifusionLogros));
        }
        return new PersonaJuridica(id, razonSocial, tipoOrganizacion, rubro, documento,
                direccion == null ? null : direccion.aDominio(), contacto,
                representantes.stream().map(r -> new Representante(r.nombre, r.email)).toList(),
                activo, fechaAlta, ultimaInteraccion, ultimoAvisoInactividad);
    }
}
