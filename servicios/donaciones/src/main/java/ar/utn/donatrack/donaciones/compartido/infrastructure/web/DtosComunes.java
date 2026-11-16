package ar.utn.donatrack.donaciones.compartido.infrastructure.web;

import ar.utn.donatrack.donaciones.compartido.domain.Coordenadas;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.compartido.domain.Pagina;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class DtosComunes {

    private DtosComunes() {
    }

    public record DireccionDto(@NotBlank String calle, String altura, @NotBlank String localidad, String provincia,
                               String codigoPostal, Double latitud, Double longitud) {

        public Direccion aDominio() {
            Coordenadas c = latitud == null || longitud == null ? null : new Coordenadas(latitud, longitud);
            return new Direccion(calle, altura, localidad, provincia, codigoPostal, c);
        }

        public static DireccionDto de(Direccion d) {
            if (d == null) {
                return null;
            }
            return new DireccionDto(d.calle(), d.altura(), d.localidad(), d.provincia(), d.codigoPostal(),
                    d.coordenadas() == null ? null : d.coordenadas().latitud(),
                    d.coordenadas() == null ? null : d.coordenadas().longitud());
        }
    }

    public record PaginaDto<T>(List<T> elementos, int pagina, int tamanio, long total, int totalPaginas) {
        public static <T> PaginaDto<T> de(Pagina<T> p) {
            return new PaginaDto<>(p.elementos(), p.numero(), p.tamanio(), p.total(), p.totalPaginas());
        }
    }
}
