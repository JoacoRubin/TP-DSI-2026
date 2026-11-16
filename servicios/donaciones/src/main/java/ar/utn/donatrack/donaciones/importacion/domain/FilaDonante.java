package ar.utn.donatrack.donaciones.importacion.domain;

import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDonante;

public record FilaDonante(int numeroLinea, TipoDonante tipo, TipoDocumento tipoDocumento, String documento,
                          String nombre, String email, String telefono) {

    /** Para personas humanas: todo menos la última palabra. */
    public String nombreDePila() {
        int i = nombre.lastIndexOf(' ');
        return i < 0 ? nombre : nombre.substring(0, i);
    }

    public String apellido() {
        int i = nombre.lastIndexOf(' ');
        return i < 0 ? "" : nombre.substring(i + 1);
    }
}
