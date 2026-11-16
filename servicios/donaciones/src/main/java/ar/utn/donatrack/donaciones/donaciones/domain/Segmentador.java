package ar.utn.donatrack.donaciones.donaciones.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.catalogo.domain.Catalogo;
import ar.utn.donatrack.donaciones.catalogo.domain.Categoria;
import ar.utn.donatrack.donaciones.catalogo.domain.Subcategoria;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain Service: convierte una carga única en N donaciones independientes.
 * Clave de agrupación: subcategoría + unidad + vencimiento + estado de uso.
 */
public class Segmentador {

    /** Estimaciones por defecto cuando no se declara peso/volumen (para la planificación de rutas). */
    static final double KG_POR_UNIDAD = 0.5;
    static final double M3_POR_UNIDAD = 0.005;

    private final Catalogo catalogo;

    public Segmentador(Catalogo catalogo) {
        this.catalogo = catalogo;
    }

    private record Clave(UUID subcategoriaId, String unidad, LocalDate vencimiento, EstadoUso estadoUso) {
    }

    public List<Donacion> segmentar(CargaDonacion carga, String responsable, LocalDate hoy, Instant ahora) {
        if (carga.bienes() == null || carga.bienes().isEmpty()) {
            throw new ReglaDeNegocioException("La carga debe incluir al menos un bien");
        }
        carga.bienes().forEach(b -> validar(b, hoy));

        Map<Clave, List<BienDeclarado>> grupos = new LinkedHashMap<>();
        for (BienDeclarado b : carga.bienes()) {
            Clave clave = new Clave(b.subcategoriaId(), normalizarUnidad(b.unidad()), b.fechaVencimiento(), b.estadoUso());
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(b);
        }

        UUID cargaId = UUID.randomUUID();
        List<Donacion> resultado = new ArrayList<>();
        grupos.forEach((clave, bienes) -> {
            Categoria categoria = catalogo.categoriaDe(clave.subcategoriaId()).orElseThrow();
            Subcategoria sub = categoria.subcategoria(clave.subcategoriaId()).orElseThrow();
            List<Bien> bienesDonacion = bienes.stream().map(b -> new Bien(b.descripcion(), b.cantidad(), b.fotoUrl())).toList();
            double peso = bienes.stream().mapToDouble(b -> b.pesoKg() != null ? b.pesoKg() : estimarPeso(b)).sum();
            double volumen = bienes.stream().mapToDouble(b -> b.volumenM3() != null ? b.volumenM3() : b.cantidad() * M3_POR_UNIDAD).sum();
            resultado.add(Donacion.registrar(UUID.randomUUID(), cargaId, carga.donanteId(), carga.descripcionGeneral(),
                    sub.id(), sub.nombre(), categoria.nombre(), clave.unidad(), clave.vencimiento(), clave.estadoUso(),
                    bienesDonacion, peso, volumen, responsable, ahora));
        });
        return resultado;
    }

    private void validar(BienDeclarado b, LocalDate hoy) {
        Categoria categoria = catalogo.categoriaDe(b.subcategoriaId())
                .orElseThrow(() -> new ReglaDeNegocioException("Subcategoría inexistente para '" + b.descripcion() + "'"));
        if (b.cantidad() <= 0) {
            throw new ReglaDeNegocioException("La cantidad de '" + b.descripcion() + "' debe ser mayor a cero");
        }
        if (categoria.perecedera()) {
            if (b.fechaVencimiento() == null) {
                throw new ReglaDeNegocioException("'" + b.descripcion() + "' es perecedero: ingresá la fecha de vencimiento");
            }
            if (b.fechaVencimiento().isBefore(hoy)) {
                throw new ReglaDeNegocioException("'" + b.descripcion() + "' ya está vencido: no puede ingresar al depósito");
            }
        }
        if (categoria.requiereEstadoUso() && b.estadoUso() == null) {
            throw new ReglaDeNegocioException("Indicá si '" + b.descripcion() + "' es nuevo o usado");
        }
    }

    private static String normalizarUnidad(String unidad) {
        return unidad == null || unidad.isBlank() ? "unidades" : unidad.trim().toLowerCase();
    }

    private static double estimarPeso(BienDeclarado b) {
        String u = normalizarUnidad(b.unidad());
        return u.equals("kg") || u.equals("kilogramos") ? b.cantidad() : b.cantidad() * KG_POR_UNIDAD;
    }
}
