package ar.utn.donatrack.donaciones.donaciones.domain;

import java.util.List;
import java.util.UUID;

/** La carga única de todos los bienes que trae una persona donante al depósito. */
public record CargaDonacion(UUID donanteId, String descripcionGeneral, List<BienDeclarado> bienes) {
}
