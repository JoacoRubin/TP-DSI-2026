package ar.utn.donatrack.logistica.rutas.application;

import java.util.List;
import java.util.UUID;

/**
 * Puerto hacia el componente EXTERNO de planificación. Es asincrónico: el resultado llega
 * después por la URL de callback.
 */
public interface PlanificadorRutas {

    void solicitar(Pedido pedido);

    record Ubicacion(double latitud, double longitud) {
    }

    record CamionDisponible(UUID id, String patente, double cargaKg, double volumenM3) {
    }

    record EntregaAPlanificar(UUID id, UUID destinoId, double latitud, double longitud, double pesoKg, double volumenM3) {
    }

    record Pedido(UUID solicitudId, Ubicacion deposito, List<CamionDisponible> camiones, List<EntregaAPlanificar> entregas) {
    }
}
