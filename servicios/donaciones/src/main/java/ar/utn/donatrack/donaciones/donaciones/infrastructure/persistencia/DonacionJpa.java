package ar.utn.donatrack.donaciones.donaciones.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.donaciones.domain.Bien;
import ar.utn.donatrack.donaciones.donaciones.domain.CambioEstado;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoUso;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La tabla de auditoría ({@code donaciones_historial}) es append-only por diseño de dominio.
 * {@code cantidadTotal} y {@code categoriaNombre} están desnormalizados para filtrar sin JOINs.
 */
@Entity
@Table(name = "donaciones", indexes = {
        @Index(name = "ix_donaciones_estado", columnList = "estado"),
        @Index(name = "ix_donaciones_donante", columnList = "donanteId,registradaEn"),
        @Index(name = "ix_donaciones_entidad", columnList = "entidadId,estado"),
        @Index(name = "ix_donaciones_entregada", columnList = "estado,entregadaEn")})
public class DonacionJpa {

    @Embeddable
    public static class BienJpa {
        @Column(length = 500)
        private String descripcion;
        private int cantidad;
        private String fotoUrl;

        protected BienJpa() {
        }

        BienJpa(Bien b) {
            this.descripcion = b.descripcion();
            this.cantidad = b.cantidad();
            this.fotoUrl = b.fotoUrl();
        }

        Bien aDominio() {
            return new Bien(descripcion, cantidad, fotoUrl);
        }
    }

    @Embeddable
    public static class CambioEstadoJpa {
        @Enumerated(EnumType.STRING)
        private EstadoDonacion desde;
        @Enumerated(EnumType.STRING)
        private EstadoDonacion hasta;
        private Instant fecha;
        private String responsable;
        @Column(length = 1000)
        private String motivo;

        protected CambioEstadoJpa() {
        }

        CambioEstadoJpa(CambioEstado c) {
            this.desde = c.desde();
            this.hasta = c.hasta();
            this.fecha = c.fecha();
            this.responsable = c.responsable();
            this.motivo = c.motivo();
        }

        CambioEstado aDominio() {
            return new CambioEstado(desde, hasta, fecha, responsable, motivo);
        }
    }

    @Id
    private UUID id;
    private UUID cargaId;
    @Column(nullable = false)
    private UUID donanteId;
    @Column(length = 1000)
    private String descripcionGeneral;
    @Column(nullable = false)
    private UUID subcategoriaId;
    private String subcategoriaNombre;
    private String categoriaNombre;
    private String unidad;
    private LocalDate fechaVencimiento;
    @Enumerated(EnumType.STRING)
    private EstadoUso estadoUso;
    private double pesoKg;
    private double volumenM3;
    private int cantidadTotal;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoDonacion estado;
    private UUID necesidadId;
    private UUID entidadId;
    private String patenteEntrega;
    private Instant entregadaEn;
    private Instant registradaEn;
    @ElementCollection
    @CollectionTable(name = "donaciones_bienes", joinColumns = @JoinColumn(name = "donacion_id"))
    @OrderColumn(name = "orden")
    private List<BienJpa> bienes = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "donaciones_historial", joinColumns = @JoinColumn(name = "donacion_id"))
    @OrderColumn(name = "orden")
    private List<CambioEstadoJpa> historial = new ArrayList<>();

    protected DonacionJpa() {
    }

    static DonacionJpa desde(Donacion d) {
        DonacionJpa j = new DonacionJpa();
        j.id = d.id();
        j.cargaId = d.cargaId();
        j.donanteId = d.donanteId();
        j.descripcionGeneral = d.descripcionGeneral();
        j.subcategoriaId = d.subcategoriaId();
        j.subcategoriaNombre = d.subcategoriaNombre();
        j.categoriaNombre = d.categoriaNombre();
        j.unidad = d.unidad();
        j.fechaVencimiento = d.fechaVencimiento();
        j.estadoUso = d.estadoUso();
        j.pesoKg = d.pesoKg();
        j.volumenM3 = d.volumenM3();
        j.cantidadTotal = d.cantidadTotal();
        j.estado = d.estado();
        j.necesidadId = d.necesidadId();
        j.entidadId = d.entidadId();
        j.patenteEntrega = d.patenteEntrega();
        j.entregadaEn = d.entregadaEn();
        j.registradaEn = d.registradaEn();
        j.bienes = new ArrayList<>(d.bienes().stream().map(BienJpa::new).toList());
        j.historial = new ArrayList<>(d.historial().stream().map(CambioEstadoJpa::new).toList());
        return j;
    }

    Donacion aDominio() {
        return new Donacion(id, cargaId, donanteId, descripcionGeneral, subcategoriaId, subcategoriaNombre,
                categoriaNombre, unidad, fechaVencimiento, estadoUso, bienes.stream().map(BienJpa::aDominio).toList(),
                pesoKg, volumenM3, estado, necesidadId, entidadId, patenteEntrega, entregadaEn, registradaEn,
                historial.stream().map(CambioEstadoJpa::aDominio).toList());
    }
}
