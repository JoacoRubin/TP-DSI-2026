package ar.utn.donatrack.donaciones.compartido.application;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;

/**
 * Colaborador compartido por las altas de donantes y entidades: crea la cuenta de acceso y,
 * si quedó pendiente de activación, envía el correo de bienvenida con el enlace.
 */
public class AltaDeCuenta {

    private final ServicioCuentas cuentas;
    private final Notificador notificador;
    private final Enlaces enlaces;

    public AltaDeCuenta(ServicioCuentas cuentas, Notificador notificador, Enlaces enlaces) {
        this.cuentas = cuentas;
        this.notificador = notificador;
        this.enlaces = enlaces;
    }

    public void crear(Destinatario destinatario, String rol, String perfil, String referencia, String clave) {
        ServicioCuentas.CuentaCreada cuenta = cuentas.crear(new ServicioCuentas.NuevaCuenta(
                destinatario.email(), destinatario.nombre(), rol, perfil, referencia, clave));
        if (cuenta.tokenActivacion() != null) {
            enviarBienvenida(destinatario, cuenta.tokenActivacion());
        }
    }

    public void enviarBienvenida(Destinatario destinatario, String token) {
        // Forzamos email: las credenciales sólo viajan por el medio obligatorio y verificable.
        notificador.notificar(Destinatario.porEmail(destinatario.nombre(), destinatario.email()),
                "¡Bienvenida/o a DonaTrack!",
                "Hola " + destinatario.nombre() + ", ya tenés una cuenta en DonaTrack para seguir tus donaciones. "
                        + "Activala y elegí tu contraseña en: " + enlaces.activacion(token)
                        + " (el enlace vence en 7 días).",
                "BIENVENIDA");
    }
}
