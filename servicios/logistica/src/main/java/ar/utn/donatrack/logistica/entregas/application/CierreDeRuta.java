package ar.utn.donatrack.logistica.entregas.application;

import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.RutaRepositorio;

import java.time.Instant;
import java.util.UUID;

/** Una ruta termina cuando todas sus entregas llegaron a un estado final. */
public class CierreDeRuta {

    private final EntregaRepositorio entregas;
    private final RutaRepositorio rutas;

    public CierreDeRuta(EntregaRepositorio entregas, RutaRepositorio rutas) {
        this.entregas = entregas;
        this.rutas = rutas;
    }

    public void verificar(UUID rutaId, Instant ahora) {
        if (rutaId == null) {
            return;
        }
        boolean completa = entregas.deRuta(rutaId).stream().map(Entrega::estado).allMatch(e -> e.esFinal());
        if (completa) {
            rutas.buscar(rutaId).ifPresent(r -> {
                r.finalizar(ahora);
                rutas.guardar(r);
            });
        }
    }
}
