package ar.utn.donatrack.donaciones.asignacion.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.asignacion.domain.PesosSemanticos;
import ar.utn.donatrack.donaciones.asignacion.domain.Puntaje;
import ar.utn.donatrack.donaciones.asignacion.domain.Ranking;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "resultados_matchmaking")
class ResultadoJpa {

    @Embeddable
    static class PuntajeJpa {
        private String algoritmo;
        private int posicion;
        private UUID necesidadId;
        private UUID entidadId;
        private double valor;

        protected PuntajeJpa() {
        }

        PuntajeJpa(String algoritmo, int posicion, Puntaje p) {
            this.algoritmo = algoritmo;
            this.posicion = posicion;
            this.necesidadId = p.necesidadId();
            this.entidadId = p.entidadId();
            this.valor = p.valor();
        }
    }

    @Id
    private UUID donacionId;
    private Instant ejecutadoEn;
    @ElementCollection
    @CollectionTable(name = "resultados_puntajes", joinColumns = @JoinColumn(name = "donacion_id"))
    @OrderColumn(name = "orden")
    private List<PuntajeJpa> puntajes = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "resultados_coincidencias", joinColumns = @JoinColumn(name = "donacion_id"))
    @OrderColumn(name = "orden")
    @Column(name = "necesidad_id")
    private List<UUID> coincidencias = new ArrayList<>();

    protected ResultadoJpa() {
    }

    static ResultadoJpa desde(ResultadoMatchmaking r) {
        ResultadoJpa j = new ResultadoJpa();
        j.donacionId = r.donacionId();
        j.ejecutadoEn = r.ejecutadoEn();
        for (Ranking rk : r.rankings()) {
            for (int i = 0; i < rk.puntajes().size(); i++) {
                j.puntajes.add(new PuntajeJpa(rk.algoritmo(), i + 1, rk.puntajes().get(i)));
            }
            if (rk.puntajes().isEmpty()) {
                j.puntajes.add(new PuntajeJpa(rk.algoritmo(), 0, new Puntaje(null, null, 0)));
            }
        }
        j.coincidencias = new ArrayList<>(r.coincidencias());
        return j;
    }

    ResultadoMatchmaking aDominio() {
        Map<String, List<Puntaje>> porAlgoritmo = new LinkedHashMap<>();
        for (PuntajeJpa p : puntajes) {
            List<Puntaje> lista = porAlgoritmo.computeIfAbsent(p.algoritmo, k -> new ArrayList<>());
            if (p.posicion > 0) {
                lista.add(new Puntaje(p.necesidadId, p.entidadId, p.valor));
            }
        }
        List<Ranking> rankings = porAlgoritmo.entrySet().stream().map(e -> new Ranking(e.getKey(), e.getValue())).toList();
        return new ResultadoMatchmaking(donacionId, ejecutadoEn, rankings, List.copyOf(coincidencias));
    }
}

@Entity
@Table(name = "configuracion_matchmaking")
class ConfiguracionJpa {
    static final String CLAVE = "pesos";
    @Id
    private String clave = CLAVE;
    private double pesoTextual;
    private double pesoVolumen;
    private double pesoTipo;

    protected ConfiguracionJpa() {
    }

    ConfiguracionJpa(PesosSemanticos p) {
        this.pesoTextual = p.textual();
        this.pesoVolumen = p.volumen();
        this.pesoTipo = p.tipo();
    }

    PesosSemanticos aDominio() {
        return new PesosSemanticos(pesoTextual, pesoVolumen, pesoTipo);
    }
}

interface ResultadoJpaRepository extends JpaRepository<ResultadoJpa, UUID> {
}

interface ConfiguracionJpaRepository extends JpaRepository<ConfiguracionJpa, String> {
}

@Repository
@Transactional
class RepositorioResultadosJpa implements ResultadoMatchmakingRepositorio {

    private final ResultadoJpaRepository resultados;
    private final ConfiguracionJpaRepository configuracion;

    RepositorioResultadosJpa(ResultadoJpaRepository resultados, ConfiguracionJpaRepository configuracion) {
        this.resultados = resultados;
        this.configuracion = configuracion;
    }

    @Override
    public void guardar(ResultadoMatchmaking resultado) {
        resultados.save(ResultadoJpa.desde(resultado));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultadoMatchmaking> deDonacion(UUID donacionId) {
        return resultados.findById(donacionId).map(ResultadoJpa::aDominio);
    }

    @Override
    public void eliminarDeDonacion(UUID donacionId) {
        if (resultados.existsById(donacionId)) {
            resultados.deleteById(donacionId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PesosSemanticos pesos() {
        return configuracion.findById(ConfiguracionJpa.CLAVE).map(ConfiguracionJpa::aDominio).orElse(PesosSemanticos.PREDETERMINADOS);
    }

    @Override
    public void guardarPesos(PesosSemanticos pesos) {
        configuracion.save(new ConfiguracionJpa(pesos));
    }
}
