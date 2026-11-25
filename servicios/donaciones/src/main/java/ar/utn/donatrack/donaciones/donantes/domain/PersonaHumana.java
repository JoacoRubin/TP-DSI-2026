package ar.utn.donatrack.donaciones.donantes.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.compartido.domain.Textos;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PersonaHumana extends Donante {

    private String nombre;
    private String apellido;
    private Integer edad;
    private Documento documento;
    private Genero genero;
    /** Consentimiento (Ley 25.326) para difundir sus logros en redes. Opt-in: arranca en no. */
    private boolean aceptaDifusionLogros;

    /** Reconstitución desde persistencia: no valida completitud (puede venir del histórico CSV). */
    public PersonaHumana(UUID id, String nombre, String apellido, Integer edad, Documento documento, Genero genero,
                         Direccion direccion, MediosContacto contacto, boolean activo, Instant fechaAlta,
                         Instant ultimaInteraccion, Instant ultimoAvisoInactividad, boolean aceptaDifusionLogros) {
        super(id, contacto, direccion, activo, fechaAlta, ultimaInteraccion, ultimoAvisoInactividad);
        this.nombre = Textos.requerido(nombre, "nombre");
        this.apellido = apellido == null ? "" : apellido.trim();
        this.edad = edad;
        this.documento = Objects.requireNonNull(documento);
        this.genero = genero;
        this.aceptaDifusionLogros = aceptaDifusionLogros;
    }

    /** Alta por formulario: se exigen TODOS los datos que pide el negocio. */
    public static PersonaHumana registrar(UUID id, String nombre, String apellido, Integer edad, Documento documento,
                                         Genero genero, Direccion direccion, MediosContacto contacto, Instant ahora) {
        Textos.requerido(apellido, "apellido");
        if (edad == null || edad < 0 || edad > 120) {
            throw new ReglaDeNegocioException("La edad es obligatoria y debe estar entre 0 y 120");
        }
        if (genero == null) {
            throw new ReglaDeNegocioException("El campo 'género' es obligatorio");
        }
        if (direccion == null) {
            throw new ReglaDeNegocioException("La dirección es obligatoria");
        }
        return new PersonaHumana(id, nombre, apellido, edad, documento, genero, direccion, contacto, true, ahora, ahora, null, false);
    }

    /** Alta desde el histórico (CSV): sólo los datos mínimos para identificar y contactar. */
    public static PersonaHumana importada(UUID id, String nombre, String apellido, Documento documento,
                                         MediosContacto contacto, Instant ahora) {
        return new PersonaHumana(id, nombre, apellido, null, documento, null, null, contacto, true, ahora, ahora, null, false);
    }

    /** Lo decide la persona en su alta; un alta hecha por terceros (administración, CSV) nunca lo otorga. */
    public void aceptarDifusionLogros(boolean acepta) {
        this.aceptaDifusionLogros = acepta;
    }

    public void actualizarDatosPersonales(String nombre, String apellido, Integer edad, Genero genero, Documento documento) {
        this.nombre = Textos.requerido(nombre, "nombre");
        this.apellido = apellido == null ? this.apellido : apellido.trim();
        if (edad != null) {
            this.edad = edad;
        }
        if (genero != null) {
            this.genero = genero;
        }
        if (documento != null) {
            this.documento = documento;
        }
    }

    @Override
    public String nombreVisible() {
        return (nombre + " " + apellido).trim();
    }

    @Override
    public TipoDonante tipo() {
        return TipoDonante.HUMANA;
    }

    @Override
    public Documento documento() {
        return documento;
    }

    public String nombre() { return nombre; }
    public String apellido() { return apellido; }
    public Integer edad() { return edad; }
    public Genero genero() { return genero; }
    public boolean aceptaDifusionLogros() { return aceptaDifusionLogros; }
}
