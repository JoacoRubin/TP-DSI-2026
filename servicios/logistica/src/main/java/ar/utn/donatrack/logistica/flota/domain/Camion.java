package ar.utn.donatrack.logistica.flota.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Camión de la flota. Persistido tal cual en MongoDB: Spring Data mapea por convención
 * (campos + único constructor), así que el dominio no necesita anotaciones.
 */
public class Camion {

    /** Patentes argentinas: formato viejo ABC123 o Mercosur AB123CD. */
    private static final Pattern PATENTE = Pattern.compile("^([A-Z]{3}\\d{3}|[A-Z]{2}\\d{3}[A-Z]{2})$");

    private final UUID id;
    private final String patente;
    private double volumenM3;
    private double alturaM;
    private double cargaKg;
    private boolean activo;

    public Camion(UUID id, String patente, double volumenM3, double alturaM, double cargaKg, boolean activo) {
        this.id = Objects.requireNonNull(id);
        this.patente = normalizarPatente(patente);
        validarCapacidades(volumenM3, alturaM, cargaKg);
        this.volumenM3 = volumenM3;
        this.alturaM = alturaM;
        this.cargaKg = cargaKg;
        this.activo = activo;
    }

    public static Camion registrar(UUID id, String patente, double volumenM3, double alturaM, double cargaKg) {
        return new Camion(id, patente, volumenM3, alturaM, cargaKg, true);
    }

    public static String normalizarPatente(String patente) {
        String p = patente == null ? "" : patente.replaceAll("[\\s-]", "").toUpperCase();
        if (!PATENTE.matcher(p).matches()) {
            throw new ReglaDeNegocioException("Patente inválida: usá el formato ABC123 o AB123CD");
        }
        return p;
    }

    private static void validarCapacidades(double volumen, double altura, double carga) {
        if (volumen <= 0 || altura <= 0 || carga <= 0) {
            throw new ReglaDeNegocioException("Volumen, altura y capacidad de carga deben ser mayores a cero");
        }
    }

    public void actualizar(double volumenM3, double alturaM, double cargaKg) {
        validarCapacidades(volumenM3, alturaM, cargaKg);
        this.volumenM3 = volumenM3;
        this.alturaM = alturaM;
        this.cargaKg = cargaKg;
    }

    public void darDeBaja() {
        this.activo = false;
    }

    public void reactivar() {
        this.activo = true;
    }

    public UUID id() { return id; }
    public String patente() { return patente; }
    public double volumenM3() { return volumenM3; }
    public double alturaM() { return alturaM; }
    public double cargaKg() { return cargaKg; }
    public boolean activo() { return activo; }
}
