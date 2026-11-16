package ar.utn.donatrack.front.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "donatrack.servicios")
public record UrlsServicios(String donaciones, String logistica, String incentivos, String notificaciones,
                            String autenticacion) {
}
