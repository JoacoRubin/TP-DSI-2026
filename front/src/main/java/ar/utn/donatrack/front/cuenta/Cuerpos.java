package ar.utn.donatrack.front.cuenta;

import java.util.Map;

/** Fachada pública de los constructores de cuerpos (usada por la sección de administración). */
public final class Cuerpos {

    private Cuerpos() {
    }

    public static Map<String, Object> personaHumana(Map<String, String> f) {
        return CuentaController.personaHumana(f, null);
    }

    public static Map<String, Object> personaJuridica(Map<String, String> f) {
        return CuentaController.personaJuridica(f, null);
    }

    public static Map<String, Object> entidad(Map<String, String> f) {
        return CuentaController.entidad(f, null);
    }
}
