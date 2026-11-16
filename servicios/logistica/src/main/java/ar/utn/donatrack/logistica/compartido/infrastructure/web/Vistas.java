package ar.utn.donatrack.logistica.compartido.infrastructure.web;

import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.rutas.domain.Parada;
import ar.utn.donatrack.logistica.rutas.domain.Posicion;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacion;
import ar.utn.donatrack.soporte.seguridad.Claims;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** DTOs de salida de la API de Logística. */
public final class Vistas {

    private Vistas() {
    }

    public static Actor actor(Authentication auth) {
        if (auth == null) {
            return new Actor("anonimo", "ANONIMO", null);
        }
        String rol = Claims.jwt(auth).map(j -> j.getClaimAsString(Claims.ROL)).orElse("ANONIMO");
        return new Actor(Claims.usuario(auth), rol, Claims.referencia(auth).orElse(null));
    }

    public record CamionVista(UUID id, String patente, double volumenM3, double alturaM, double cargaKg, boolean activo) {
        public static CamionVista de(Camion c) {
            return new CamionVista(c.id(), c.patente(), c.volumenM3(), c.alturaM(), c.cargaKg(), c.activo());
        }
    }

    public record HistorialVista(String estado, Instant fecha, String detalle) {
    }

    public record EntregaVista(UUID id, UUID donacionId, UUID donanteId, String donanteNombre, UUID entidadId,
                               String entidadNombre, String direccion, double latitud, double longitud,
                               String descripcion, String subcategoria, int cantidad, String unidad, double pesoKg,
                               double volumenM3, String estado, String estadoEtiqueta, UUID rutaId, String patente,
                               List<String> fotos, String motivoNoRecepcion, Instant entregadaEn, Instant creadaEn,
                               List<HistorialVista> historial) {
        public static EntregaVista de(Entrega e) {
            return new EntregaVista(e.id(), e.donacionId(), e.donanteId(), e.donanteNombre(), e.entidadId(),
                    e.destino().entidadNombre(), e.destino().direccion(), e.destino().latitud(), e.destino().longitud(),
                    e.detalle() == null ? null : e.detalle().descripcion(), e.detalle() == null ? null : e.detalle().subcategoria(),
                    e.detalle() == null ? 0 : e.detalle().cantidad(), e.detalle() == null ? null : e.detalle().unidad(),
                    e.pesoKg(), e.volumenM3(), e.estado().name(), e.estado().etiqueta(), e.rutaId(), e.patente(), e.fotos(),
                    e.motivoNoRecepcion(), e.entregadaEn(), e.creadaEn(),
                    e.historial().stream().map(h -> new HistorialVista(h.estado().etiqueta(), h.fecha(), h.detalle())).toList());
        }
    }

    /** Vista pública (sin login): SIN datos del donante (Ley 25.326 de Protección de Datos Personales). */
    public record EntregaPublicaVista(UUID id, String descripcion, String subcategoria, String entidadNombre,
                                      double latitud, double longitud, Instant entregadaEn, List<String> fotos) {
        public static EntregaPublicaVista de(Entrega e) {
            return new EntregaPublicaVista(e.id(), e.detalle() == null ? null : e.detalle().descripcion(),
                    e.detalle() == null ? null : e.detalle().subcategoria(), e.destino().entidadNombre(),
                    e.destino().latitud(), e.destino().longitud(), e.entregadaEn(), e.fotos());
        }
    }

    public record PosicionVista(double latitud, double longitud, double velocidadKmh, Instant registradaEn) {
        static PosicionVista de(Posicion p) {
            return p == null ? null : new PosicionVista(p.latitud(), p.longitud(), p.velocidadKmh(), p.registradaEn());
        }
    }

    public record EntregaEnRutaVista(UUID entregaId, UUID donacionId, String descripcion, boolean propia) {
    }

    public record ParadaVista(int orden, UUID entidadId, String entidadNombre, String direccion, double latitud,
                              double longitud, List<EntregaEnRutaVista> entregas) {
    }

    public record RutaVista(UUID id, LocalDate fecha, String patente, String estado, Instant iniciadaEn,
                            Instant finalizadaEn, PosicionVista ultimaPosicion, List<ParadaVista> paradas,
                            List<double[]> recorrido, int totalEntregas) {

        /** Completa (para administración y chofer). */
        public static RutaVista de(Ruta r) {
            return filtrada(r, null);
        }

        /**
         * Para donantes y entidades: sólo ven las paradas que les corresponden. Privacidad: un
         * donante no necesita conocer la dirección de otras entidades.
         */
        public static RutaVista filtrada(Ruta r, Actor actor) {
            List<ParadaVista> paradas = r.paradas().stream()
                    .filter(p -> actor == null || actor.esAdmin() || visible(p, actor))
                    .map(p -> new ParadaVista(p.orden(), p.entidadId(), p.destino().entidadNombre(), p.destino().direccion(),
                            p.destino().latitud(), p.destino().longitud(), p.entregas().stream()
                            .filter(e -> actor == null || actor.esAdmin() || propia(e, p, actor))
                            .map(e -> new EntregaEnRutaVista(e.entregaId(), e.donacionId(), e.descripcion(), actor != null && propia(e, p, actor)))
                            .toList()))
                    .toList();
            return new RutaVista(r.id(), r.fecha(), r.patente(), r.estado().name(), r.iniciadaEn(), r.finalizadaEn(),
                    PosicionVista.de(r.ultimaPosicion()), paradas,
                    r.recorrido().stream().map(p -> new double[]{p.latitud(), p.longitud()}).toList(),
                    r.entregaIds().size());
        }

        private static boolean visible(Parada p, Actor actor) {
            return p.entregas().stream().anyMatch(e -> propia(e, p, actor));
        }

        private static boolean propia(Parada.EntregaEnRuta e, Parada p, Actor actor) {
            String ref = actor.referencia();
            return switch (actor.rol()) {
                case "DONANTE" -> e.donanteId() != null && e.donanteId().toString().equals(ref);
                case "BENEFICIARIA" -> p.entidadId() != null && p.entidadId().toString().equals(ref);
                default -> false;
            };
        }
    }

    public record SolicitudVista(UUID id, LocalDate fechaRutas, int entregas, String estado, Instant enviadaEn,
                                 Instant completadaEn, int rutasGeneradas, int sinAsignar) {
        public static SolicitudVista de(SolicitudPlanificacion s) {
            return new SolicitudVista(s.id(), s.fechaRutas(), s.entregaIds().size(), s.estado().name(), s.enviadaEn(),
                    s.completadaEn(), s.rutasGeneradas(), s.sinAsignar());
        }
    }
}
