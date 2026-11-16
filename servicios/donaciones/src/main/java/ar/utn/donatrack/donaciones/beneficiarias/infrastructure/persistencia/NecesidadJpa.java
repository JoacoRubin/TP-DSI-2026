package ar.utn.donatrack.donaciones.beneficiarias.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Periodicidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.TipoNecesidad;
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
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "necesidades", indexes = {
        @Index(name = "ix_necesidades_subcat", columnList = "subcategoriaId,activa"),
        @Index(name = "ix_necesidades_entidad", columnList = "entidadId")})
public class NecesidadJpa {

    @Embeddable
    public static class RecepcionJpa {
        private LocalDate fecha;
        private int cantidad;

        protected RecepcionJpa() {
        }

        RecepcionJpa(LocalDate fecha, int cantidad) {
            this.fecha = fecha;
            this.cantidad = cantidad;
        }
    }

    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoNecesidad tipo;
    @Column(nullable = false)
    private UUID entidadId;
    @Column(nullable = false)
    private UUID subcategoriaId;
    private String subcategoriaNombre;
    @Column(length = 1000)
    private String descripcion;
    private int cantidadRequerida;
    private String unidad;
    private int cantidadComprometida;
    private int cantidadRecibida;
    @Enumerated(EnumType.STRING)
    private Periodicidad periodicidad;
    @ElementCollection
    @CollectionTable(name = "necesidades_recepciones", joinColumns = @JoinColumn(name = "necesidad_id"))
    private List<RecepcionJpa> recepciones = new ArrayList<>();
    private boolean activa;
    private Instant creadaEn;

    protected NecesidadJpa() {
    }

    static NecesidadJpa desde(Necesidad n) {
        NecesidadJpa j = new NecesidadJpa();
        j.id = n.id();
        j.tipo = n.tipo();
        j.entidadId = n.entidadId();
        j.subcategoriaId = n.subcategoriaId();
        j.subcategoriaNombre = n.subcategoriaNombre();
        j.descripcion = n.descripcion();
        j.cantidadRequerida = n.cantidadRequerida();
        j.unidad = n.unidad();
        j.cantidadComprometida = n.cantidadComprometida();
        j.activa = n.activa();
        j.creadaEn = n.creadaEn();
        if (n instanceof NecesidadExtraordinaria e) {
            j.cantidadRecibida = e.cantidadRecibidaTotal();
        } else if (n instanceof NecesidadRecurrente r) {
            j.periodicidad = r.periodicidad();
            j.recepciones = new ArrayList<>(r.recepciones().stream().map(x -> new RecepcionJpa(x.fecha(), x.cantidad())).toList());
        }
        return j;
    }

    Necesidad aDominio() {
        return switch (tipo) {
            case EXTRAORDINARIA -> new NecesidadExtraordinaria(id, entidadId, subcategoriaId, subcategoriaNombre,
                    descripcion, cantidadRequerida, unidad, cantidadComprometida, cantidadRecibida, activa, creadaEn);
            case RECURRENTE -> new NecesidadRecurrente(id, entidadId, subcategoriaId, subcategoriaNombre, descripcion,
                    cantidadRequerida, unidad, cantidadComprometida, periodicidad,
                    recepciones.stream().map(r -> new NecesidadRecurrente.Recepcion(r.fecha, r.cantidad)).toList(),
                    activa, creadaEn);
        };
    }
}
