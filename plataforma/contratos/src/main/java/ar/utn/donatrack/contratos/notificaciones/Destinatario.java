package ar.utn.donatrack.contratos.notificaciones;

/**
 * Datos de contacto con los que el Servicio de Notificaciones puede llegar a una persona.
 * El servicio emisor (dueño de los datos de contacto) es quien lo arma: Notificaciones
 * no conoce donantes ni entidades, sólo destinatarios.
 */
public record Destinatario(
        String nombre,
        String email,
        String telefono,
        String whatsapp,
        MedioNotificacion medioPreferido) {

    public Destinatario {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El destinatario debe tener email");
        }
        if (medioPreferido == null) {
            medioPreferido = MedioNotificacion.EMAIL;
        }
    }

    public static Destinatario porEmail(String nombre, String email) {
        return new Destinatario(nombre, email, null, null, MedioNotificacion.EMAIL);
    }
}
