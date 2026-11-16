package ar.utn.donatrack.donaciones;

import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Periodicidad;
import ar.utn.donatrack.donaciones.catalogo.domain.Catalogo;
import ar.utn.donatrack.donaciones.catalogo.domain.Categoria;
import ar.utn.donatrack.donaciones.catalogo.domain.Subcategoria;
import ar.utn.donatrack.donaciones.compartido.domain.Coordenadas;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.donaciones.domain.Bien;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Datos de prueba reutilizables (Object Mother). */
public final class Fixtures {

    public static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    public static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    public static final Subcategoria SILLAS = new Subcategoria(UUID.randomUUID(), "Sillas");
    public static final Subcategoria MESAS = new Subcategoria(UUID.randomUUID(), "Mesas");
    public static final Subcategoria FIDEOS = new Subcategoria(UUID.randomUUID(), "Fideos secos");
    public static final Subcategoria TOMATE = new Subcategoria(UUID.randomUUID(), "Puré de tomate");
    public static final Subcategoria CAMPERAS = new Subcategoria(UUID.randomUUID(), "Camperas de abrigo");

    public static final Categoria MOBILIARIO = new Categoria(UUID.randomUUID(), "Mobiliario", false, true, List.of(SILLAS, MESAS));
    public static final Categoria ALIMENTOS = new Categoria(UUID.randomUUID(), "Alimentos", true, false, List.of(FIDEOS, TOMATE));
    public static final Categoria VESTIMENTA = new Categoria(UUID.randomUUID(), "Vestimenta", false, true, List.of(CAMPERAS));

    public static final Catalogo CATALOGO = new Catalogo(List.of(MOBILIARIO, ALIMENTOS, VESTIMENTA));

    private Fixtures() {
    }

    public static Direccion direccion() {
        return new Direccion("Medrano", "951", "CABA", "Buenos Aires", "C1179AAQ", new Coordenadas(-34.5986, -58.4202));
    }

    public static EntidadBeneficiaria entidad(String nombre) {
        return EntidadBeneficiaria.registrar(UUID.randomUUID(), nombre, direccion(), "+54 11 4444-4444",
                List.of(nombre.toLowerCase().replace(' ', '.') + "@mail.com"), AHORA);
    }

    public static NecesidadExtraordinaria extraordinaria(EntidadBeneficiaria e, Subcategoria s, String descripcion, int cantidad) {
        return NecesidadExtraordinaria.nueva(UUID.randomUUID(), e.id(), s.id(), s.nombre(), descripcion, cantidad, "unidades", AHORA);
    }

    public static NecesidadRecurrente recurrente(EntidadBeneficiaria e, Subcategoria s, String descripcion, int cantidad) {
        return NecesidadRecurrente.nueva(UUID.randomUUID(), e.id(), s.id(), s.nombre(), descripcion, cantidad, "unidades",
                Periodicidad.SEMANAL, AHORA);
    }

    public static Donacion donacion(Subcategoria s, String descripcionBien, int cantidad) {
        Categoria c = CATALOGO.categoriaDe(s.id()).orElseThrow();
        return Donacion.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Donación de prueba",
                s.id(), s.nombre(), c.nombre(), "unidades", null, null,
                List.of(new Bien(descripcionBien, cantidad, null)), cantidad, cantidad * 0.01, "admin", AHORA);
    }

    public static Necesidad necesidadDe(Donacion d, EntidadBeneficiaria e, int cantidad) {
        return NecesidadExtraordinaria.nueva(UUID.randomUUID(), e.id(), d.subcategoriaId(), d.subcategoriaNombre(),
                "necesidad", cantidad, "unidades", AHORA);
    }
}
