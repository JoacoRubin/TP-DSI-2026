package ar.utn.donatrack.donaciones.compartido.infrastructure.integraciones;

import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import org.springframework.web.client.RestClient;

import java.util.UUID;

public class ServicioLogisticaRest implements ServicioLogistica {

    private final RestClient cliente;

    public ServicioLogisticaRest(RestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public void solicitarEntrega(SolicitudEntrega solicitud) {
        ErroresRemotos.invocar("logística", () ->
                cliente.post().uri("/api/entregas").body(solicitud).retrieve().toBodilessEntity());
    }

    @Override
    public void cancelarEntrega(UUID donacionId) {
        ErroresRemotos.invocar("logística", () ->
                cliente.delete().uri("/api/entregas/por-donacion/{id}", donacionId).retrieve().toBodilessEntity());
    }
}
