package ar.utn.donatrack.incentivos.misiones.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;

import java.util.UUID;

/** El donante decide si sus insignias son visibles en su perfil público (y se difunden). */
public class ActualizarPreferencias {

    private final Repositorios.PerfilRepositorio perfiles;

    public ActualizarPreferencias(Repositorios.PerfilRepositorio perfiles) {
        this.perfiles = perfiles;
    }

    public PerfilDonante ejecutar(UUID donanteId, boolean insigniasVisibles) {
        PerfilDonante p = perfiles.buscar(donanteId).orElseThrow(() -> new RecursoNoEncontradoException("Perfil", donanteId));
        p.configurarVisibilidad(insigniasVisibles);
        perfiles.guardar(p);
        return p;
    }
}
