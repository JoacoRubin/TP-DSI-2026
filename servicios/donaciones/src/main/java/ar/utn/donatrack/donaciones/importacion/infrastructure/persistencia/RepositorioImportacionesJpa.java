package ar.utn.donatrack.donaciones.importacion.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import ar.utn.donatrack.donaciones.importacion.domain.ImportacionRepositorio;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "importaciones")
class ImportacionJpa {

    @Embeddable
    static class ErrorJpa {
        private int linea;
        @Column(length = 500)
        private String mensaje;

        protected ErrorJpa() {
        }

        ErrorJpa(int linea, String mensaje) {
            this.linea = linea;
            this.mensaje = mensaje == null ? null : mensaje.substring(0, Math.min(500, mensaje.length()));
        }
    }

    @Id
    private UUID id;
    private String archivo;
    @Enumerated(EnumType.STRING)
    private Importacion.Estado estado;
    private int filasProcesadas;
    private int creados;
    private int actualizados;
    private int conError;
    @ElementCollection
    @CollectionTable(name = "importaciones_errores", joinColumns = @JoinColumn(name = "importacion_id"))
    private List<ErrorJpa> errores = new ArrayList<>();
    private Instant iniciadaEn;
    private Instant finalizadaEn;
    @Column(length = 1000)
    private String detalleFallo;

    protected ImportacionJpa() {
    }

    static ImportacionJpa desde(Importacion i) {
        ImportacionJpa j = new ImportacionJpa();
        j.id = i.id();
        j.archivo = i.archivo();
        j.estado = i.estado();
        j.filasProcesadas = i.filasProcesadas();
        j.creados = i.creados();
        j.actualizados = i.actualizados();
        j.conError = i.conError();
        j.errores = new ArrayList<>(i.errores().stream().map(e -> new ErrorJpa(e.linea(), e.mensaje())).toList());
        j.iniciadaEn = i.iniciadaEn();
        j.finalizadaEn = i.finalizadaEn();
        j.detalleFallo = i.detalleFallo() == null ? null : i.detalleFallo().substring(0, Math.min(1000, i.detalleFallo().length()));
        return j;
    }

    Importacion aDominio() {
        return new Importacion(id, archivo, estado, filasProcesadas, creados, actualizados, conError,
                errores.stream().map(e -> new Importacion.ErrorFila(e.linea, e.mensaje)).toList(),
                iniciadaEn, finalizadaEn, detalleFallo);
    }
}

interface ImportacionJpaRepository extends JpaRepository<ImportacionJpa, UUID> {
    List<ImportacionJpa> findAllByOrderByIniciadaEnDesc(Pageable pagina);

    List<ImportacionJpa> findByEstado(Importacion.Estado estado);
}

/** El progreso se guarda en su propia transacción para que sea visible mientras el proceso corre. */
@Repository
class RepositorioImportacionesJpa implements ImportacionRepositorio {

    private final ImportacionJpaRepository jpa;

    RepositorioImportacionesJpa(ImportacionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardar(Importacion importacion) {
        jpa.save(ImportacionJpa.desde(importacion));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Importacion> buscar(UUID id) {
        return jpa.findById(id).map(ImportacionJpa::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Importacion> recientes(int limite) {
        return jpa.findAllByOrderByIniciadaEnDesc(PageRequest.of(0, limite)).stream().map(ImportacionJpa::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Importacion> enCurso() {
        return jpa.findByEstado(Importacion.Estado.EN_CURSO).stream().map(ImportacionJpa::aDominio).toList();
    }
}
