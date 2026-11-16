package ar.utn.donatrack.donaciones.compartido.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.compartido.domain.Coordenadas;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import jakarta.persistence.Embeddable;

@Embeddable
public class DireccionJpa {

    private String calle;
    private String altura;
    private String localidad;
    private String provincia;
    private String codigoPostal;
    private Double latitud;
    private Double longitud;

    protected DireccionJpa() {
    }

    public static DireccionJpa desde(Direccion d) {
        if (d == null) {
            return null;
        }
        DireccionJpa j = new DireccionJpa();
        j.calle = d.calle();
        j.altura = d.altura();
        j.localidad = d.localidad();
        j.provincia = d.provincia();
        j.codigoPostal = d.codigoPostal();
        if (d.coordenadas() != null) {
            j.latitud = d.coordenadas().latitud();
            j.longitud = d.coordenadas().longitud();
        }
        return j;
    }

    public Direccion aDominio() {
        if (calle == null) {
            return null;
        }
        Coordenadas c = latitud == null || longitud == null ? null : new Coordenadas(latitud, longitud);
        return new Direccion(calle, altura, localidad, provincia, codigoPostal, c);
    }
}
