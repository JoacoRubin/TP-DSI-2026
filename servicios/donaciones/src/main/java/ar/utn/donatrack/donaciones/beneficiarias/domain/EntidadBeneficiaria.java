package ar.utn.donatrack.donaciones.beneficiarias.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.compartido.domain.Textos;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Organización sin fines de lucro que recibe donaciones. */
public class EntidadBeneficiaria {

    private final UUID id;
    private String razonSocial;
    private Direccion direccion;
    private String telefono;
    private List<String> emailsRepresentantes;
    private boolean activa;
    private final Instant fechaAlta;

    public EntidadBeneficiaria(UUID id, String razonSocial, Direccion direccion, String telefono,
                               List<String> emailsRepresentantes, boolean activa, Instant fechaAlta) {
        this.id = Objects.requireNonNull(id);
        this.razonSocial = Textos.requerido(razonSocial, "razón social");
        this.direccion = validarDireccion(direccion);
        this.telefono = Textos.requerido(telefono, "teléfono");
        this.emailsRepresentantes = validarEmails(emailsRepresentantes);
        this.activa = activa;
        this.fechaAlta = fechaAlta;
    }

    public static EntidadBeneficiaria registrar(UUID id, String razonSocial, Direccion direccion, String telefono,
                                                List<String> emails, Instant ahora) {
        return new EntidadBeneficiaria(id, razonSocial, direccion, telefono, emails, true, ahora);
    }

    /** Las entregas se planifican por ubicación: sin coordenadas no hay ruta posible. */
    private static Direccion validarDireccion(Direccion d) {
        if (d == null || d.coordenadas() == null) {
            throw new ReglaDeNegocioException("La dirección completa (con ubicación en el mapa) es obligatoria");
        }
        return d;
    }

    private static List<String> validarEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            throw new ReglaDeNegocioException("Se requiere al menos un correo de representante");
        }
        return emails.stream().map(MediosContacto::validarEmail).distinct().toList();
    }

    public void actualizar(String razonSocial, Direccion direccion, String telefono, List<String> emails) {
        this.razonSocial = Textos.requerido(razonSocial, "razón social");
        this.direccion = validarDireccion(direccion);
        this.telefono = Textos.requerido(telefono, "teléfono");
        this.emailsRepresentantes = validarEmails(emails);
    }

    public void darDeBaja() {
        this.activa = false;
    }

    public Destinatario comoDestinatario() {
        return new Destinatario(razonSocial, emailsRepresentantes.get(0), telefono, null, MedioNotificacion.EMAIL);
    }

    public UUID id() { return id; }
    public String razonSocial() { return razonSocial; }
    public Direccion direccion() { return direccion; }
    public String telefono() { return telefono; }
    public List<String> emailsRepresentantes() { return emailsRepresentantes; }
    public String emailPrincipal() { return emailsRepresentantes.get(0); }
    public boolean activa() { return activa; }
    public Instant fechaAlta() { return fechaAlta; }
}
