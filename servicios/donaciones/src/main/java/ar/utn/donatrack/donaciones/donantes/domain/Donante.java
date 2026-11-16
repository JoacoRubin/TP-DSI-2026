package ar.utn.donatrack.donaciones.donantes.domain;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persona donante (humana o jurídica). Comportamiento común: contacto, interacción e
 * inactividad. Las subclases definen identidad y datos propios (herencia por especialización).
 */
public abstract class Donante {

    protected final UUID id;
    protected MediosContacto contacto;
    protected Direccion direccion;
    protected boolean activo;
    protected final Instant fechaAlta;
    protected Instant ultimaInteraccion;
    protected Instant ultimoAvisoInactividad;

    protected Donante(UUID id, MediosContacto contacto, Direccion direccion, boolean activo, Instant fechaAlta,
                      Instant ultimaInteraccion, Instant ultimoAvisoInactividad) {
        this.id = Objects.requireNonNull(id);
        this.contacto = Objects.requireNonNull(contacto);
        this.direccion = direccion;
        this.activo = activo;
        this.fechaAlta = fechaAlta;
        this.ultimaInteraccion = ultimaInteraccion != null ? ultimaInteraccion : fechaAlta;
        this.ultimoAvisoInactividad = ultimoAvisoInactividad;
    }

    public abstract String nombreVisible();

    public abstract TipoDonante tipo();

    public abstract Documento documento();

    public Destinatario comoDestinatario() {
        return new Destinatario(nombreVisible(), contacto.email(), contacto.telefono(), contacto.whatsapp(),
                contacto.preferido());
    }

    public void registrarInteraccion(Instant momento) {
        if (ultimaInteraccion == null || momento.isAfter(ultimaInteraccion)) {
            ultimaInteraccion = momento;
        }
    }

    /** Sin interacción durante más que el umbral y todavía no avisado desde la última interacción. */
    public boolean requiereAvisoInactividad(Instant ahora, Duration umbral) {
        boolean inactivo = ultimaInteraccion.plus(umbral).isBefore(ahora);
        boolean yaAvisado = ultimoAvisoInactividad != null && !ultimoAvisoInactividad.isBefore(ultimaInteraccion);
        return activo && inactivo && !yaAvisado;
    }

    public void registrarAvisoInactividad(Instant momento) {
        this.ultimoAvisoInactividad = momento;
    }

    public void actualizarContacto(MediosContacto nuevo) {
        this.contacto = Objects.requireNonNull(nuevo);
    }

    public void actualizarDireccion(Direccion nueva) {
        this.direccion = nueva;
    }

    public void darDeBaja() {
        this.activo = false;
    }

    public UUID id() { return id; }
    public MediosContacto contacto() { return contacto; }
    public String email() { return contacto.email(); }
    public Direccion direccion() { return direccion; }
    public boolean activo() { return activo; }
    public Instant fechaAlta() { return fechaAlta; }
    public Instant ultimaInteraccion() { return ultimaInteraccion; }
    public Instant ultimoAvisoInactividad() { return ultimoAvisoInactividad; }
}
