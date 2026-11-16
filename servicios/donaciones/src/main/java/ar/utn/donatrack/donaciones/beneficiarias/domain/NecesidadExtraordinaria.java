package ar.utn.donatrack.donaciones.beneficiarias.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Situación puntual (inundación, mudanza...). Se satisface con donaciones parciales acumuladas. */
public class NecesidadExtraordinaria extends Necesidad {

    private int cantidadRecibida;

    public NecesidadExtraordinaria(UUID id, UUID entidadId, UUID subcategoriaId, String subcategoriaNombre,
                                   String descripcion, int cantidadRequerida, String unidad, int cantidadComprometida,
                                   int cantidadRecibida, boolean activa, Instant creadaEn) {
        super(id, entidadId, subcategoriaId, subcategoriaNombre, descripcion, cantidadRequerida, unidad,
                cantidadComprometida, activa, creadaEn);
        this.cantidadRecibida = cantidadRecibida;
    }

    public static NecesidadExtraordinaria nueva(UUID id, UUID entidadId, UUID subcategoriaId, String subcategoriaNombre,
                                                String descripcion, int cantidad, String unidad, Instant ahora) {
        return new NecesidadExtraordinaria(id, entidadId, subcategoriaId, subcategoriaNombre, descripcion, cantidad,
                unidad, 0, 0, true, ahora);
    }

    @Override
    public TipoNecesidad tipo() {
        return TipoNecesidad.EXTRAORDINARIA;
    }

    @Override
    public int cantidadRecibida(LocalDate hoy) {
        return cantidadRecibida;
    }

    @Override
    protected void acumularRecepcion(int cantidad, LocalDate fecha) {
        cantidadRecibida += cantidad;
    }

    public int cantidadRecibidaTotal() {
        return cantidadRecibida;
    }
}
