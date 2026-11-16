package ar.utn.donatrack.donaciones.donantes.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.compartido.domain.Textos;

import java.util.regex.Pattern;

/**
 * Email obligatorio; teléfono y WhatsApp opcionales. El medio predeterminado debe tener datos.
 */
public record MediosContacto(String email, String telefono, String whatsapp, MedioNotificacion preferido) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public MediosContacto {
        email = validarEmail(email);
        telefono = Textos.opcional(telefono);
        whatsapp = Textos.opcional(whatsapp);
        if (preferido == null) {
            preferido = MedioNotificacion.EMAIL;
        }
        if (preferido == MedioNotificacion.SMS && telefono == null) {
            throw new ReglaDeNegocioException("Para recibir notificaciones por SMS cargá un teléfono");
        }
        if (preferido == MedioNotificacion.WHATSAPP && whatsapp == null) {
            throw new ReglaDeNegocioException("Para recibir notificaciones por WhatsApp cargá un número de WhatsApp");
        }
    }

    public static String validarEmail(String email) {
        String normalizado = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL.matcher(normalizado).matches()) {
            throw new ReglaDeNegocioException("El correo electrónico es obligatorio y debe ser válido");
        }
        return normalizado;
    }
}
