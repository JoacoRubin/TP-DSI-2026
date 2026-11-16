package ar.utn.donatrack.donaciones.compartido.infrastructure.integraciones;

import ar.utn.donatrack.donaciones.compartido.application.ServicioCuentas;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

public class ServicioCuentasRest implements ServicioCuentas {

    private final RestClient cliente;

    public ServicioCuentasRest(RestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public CuentaCreada crear(NuevaCuenta cuenta) {
        RespuestaAlta r = ErroresRemotos.invocar("autenticación", () ->
                cliente.post().uri("/api/usuarios").body(cuenta).retrieve().body(RespuestaAlta.class));
        return new CuentaCreada(r.email(), r.creado(), r.tokenActivacion());
    }

    @Override
    public List<CuentaCreada> crearEnLote(List<NuevaCuenta> cuentas) {
        List<RespuestaAlta> r = ErroresRemotos.invocar("autenticación", () ->
                cliente.post().uri("/api/usuarios/lote").body(cuentas).retrieve()
                        .body(new ParameterizedTypeReference<List<RespuestaAlta>>() { }));
        return r.stream().map(x -> new CuentaCreada(x.email(), x.creado(), x.tokenActivacion())).toList();
    }

    record RespuestaAlta(String usuarioId, String email, boolean creado, String tokenActivacion) {
    }
}
