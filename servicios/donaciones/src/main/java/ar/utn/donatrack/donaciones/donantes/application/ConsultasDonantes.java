package ar.utn.donatrack.donaciones.donantes.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.compartido.domain.Pagina;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.util.UUID;

/** Lado de lectura (queries): sin efectos, agrupadas por conveniencia. */
public class ConsultasDonantes {

    private final DonanteRepositorio donantes;

    public ConsultasDonantes(DonanteRepositorio donantes) {
        this.donantes = donantes;
    }

    public Donante detalle(UUID id, Solicitante solicitante) {
        solicitante.exigirAdminODonante(id);
        return donantes.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Donante", id));
    }

    public Pagina<Donante> listar(String texto, int pagina, int tamanio) {
        return donantes.listar(texto, pagina, Math.min(Math.max(tamanio, 1), 100));
    }
}
