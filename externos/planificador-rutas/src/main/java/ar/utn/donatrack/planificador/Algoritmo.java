package ar.utn.donatrack.planificador;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Heurística simple (suficiente para simular al proveedor):
 * 1) asignación first-fit por capacidad de peso y volumen, agrupando por destino;
 * 2) orden de paradas por vecino más cercano desde el depósito (distancia haversine).
 * Lo que no entra en ningún camión vuelve como "sinAsignar".
 */
final class Algoritmo {

    record Punto(double latitud, double longitud) {
    }

    record Camion(UUID id, String patente, double cargaKg, double volumenM3) {
    }

    record Entrega(UUID id, UUID destinoId, double latitud, double longitud, double pesoKg, double volumenM3) {
    }

    record Parada(int orden, List<UUID> entregaIds) {
    }

    record Ruta(UUID camionId, List<Parada> paradas) {
    }

    record Resultado(List<Ruta> rutas, List<UUID> sinAsignar) {
    }

    private Algoritmo() {
    }

    static Resultado planificar(Punto deposito, List<Camion> camiones, List<Entrega> entregas) {
        Map<UUID, List<Entrega>> asignadas = new LinkedHashMap<>();
        Map<UUID, double[]> restante = new LinkedHashMap<>();
        camiones.forEach(c -> {
            asignadas.put(c.id(), new ArrayList<>());
            restante.put(c.id(), new double[]{c.cargaKg(), c.volumenM3()});
        });
        List<UUID> sinAsignar = new ArrayList<>();

        Map<UUID, List<Entrega>> porDestino = entregas.stream()
                .collect(Collectors.groupingBy(Entrega::destinoId, LinkedHashMap::new, Collectors.toList()));
        for (List<Entrega> grupo : porDestino.values()) {
            for (Entrega e : grupo.stream().sorted(Comparator.comparingDouble(Entrega::pesoKg).reversed()).toList()) {
                UUID elegido = null;
                // Preferimos el camión que ya va a ese destino (menos paradas)
                for (Camion c : camiones) {
                    double[] r = restante.get(c.id());
                    boolean yaVa = asignadas.get(c.id()).stream().anyMatch(x -> x.destinoId().equals(e.destinoId()));
                    if (yaVa && r[0] >= e.pesoKg() && r[1] >= e.volumenM3()) {
                        elegido = c.id();
                        break;
                    }
                }
                if (elegido == null) {
                    for (Camion c : camiones) {
                        double[] r = restante.get(c.id());
                        if (r[0] >= e.pesoKg() && r[1] >= e.volumenM3()) {
                            elegido = c.id();
                            break;
                        }
                    }
                }
                if (elegido == null) {
                    sinAsignar.add(e.id());
                } else {
                    asignadas.get(elegido).add(e);
                    restante.get(elegido)[0] -= e.pesoKg();
                    restante.get(elegido)[1] -= e.volumenM3();
                }
            }
        }

        List<Ruta> rutas = new ArrayList<>();
        asignadas.forEach((camionId, lista) -> {
            if (!lista.isEmpty()) {
                rutas.add(new Ruta(camionId, ordenarParadas(deposito, lista)));
            }
        });
        return new Resultado(rutas, sinAsignar);
    }

    private static List<Parada> ordenarParadas(Punto deposito, List<Entrega> entregas) {
        Map<UUID, List<Entrega>> porDestino = entregas.stream()
                .collect(Collectors.groupingBy(Entrega::destinoId, LinkedHashMap::new, Collectors.toList()));
        List<List<Entrega>> pendientes = new ArrayList<>(porDestino.values());
        List<Parada> paradas = new ArrayList<>();
        Punto actual = deposito;
        while (!pendientes.isEmpty()) {
            Punto desde = actual;
            List<Entrega> cercano = pendientes.stream()
                    .min(Comparator.comparingDouble(g -> distanciaKm(desde, new Punto(g.get(0).latitud(), g.get(0).longitud()))))
                    .orElseThrow();
            pendientes.remove(cercano);
            paradas.add(new Parada(paradas.size() + 1, cercano.stream().map(Entrega::id).toList()));
            actual = new Punto(cercano.get(0).latitud(), cercano.get(0).longitud());
        }
        return paradas;
    }

    static double distanciaKm(Punto a, Punto b) {
        double r = 6371;
        double dLat = Math.toRadians(b.latitud() - a.latitud());
        double dLon = Math.toRadians(b.longitud() - a.longitud());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(a.latitud()))
                * Math.cos(Math.toRadians(b.latitud())) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }
}
