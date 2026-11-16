package ar.utn.donatrack.donaciones.compartido.application;

/** URLs públicas del front que viajan dentro de las notificaciones. */
public record Enlaces(String baseFront) {

    public String activacion(String token) {
        return baseFront + "/activar?token=" + token;
    }

    public String seguimientoDonante() {
        return baseFront + "/donante/seguimiento";
    }

    public String seguimientoEntidad() {
        return baseFront + "/entidad/seguimiento";
    }

    public String misDonaciones() {
        return baseFront + "/donante/donaciones";
    }
}
