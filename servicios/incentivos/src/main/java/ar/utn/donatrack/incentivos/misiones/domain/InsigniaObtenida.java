package ar.utn.donatrack.incentivos.misiones.domain;

import java.time.Instant;
import java.util.UUID;

public record InsigniaObtenida(UUID id, UUID donanteId, String misionId, String nombre, String icono,
                               CategoriaDonante categoria, Instant obtenidaEn) {

    public static InsigniaObtenida nueva(UUID donanteId, String misionId, String nombre, String icono,
                                         CategoriaDonante categoria, Instant ahora) {
        return new InsigniaObtenida(UUID.randomUUID(), donanteId, misionId, nombre, icono, categoria, ahora);
    }
}
