package ar.utn.donatrack.donaciones.importacion.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.donaciones.compartido.application.AltaDeCuenta;
import ar.utn.donatrack.donaciones.compartido.application.ServicioCuentas;
import ar.utn.donatrack.donaciones.compartido.application.Transaccion;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaJuridica;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDonante;
import ar.utn.donatrack.donaciones.importacion.domain.FilaDonante;
import ar.utn.donatrack.donaciones.importacion.domain.FormatoCsvDonantes;
import ar.utn.donatrack.donaciones.importacion.domain.Importacion;
import ar.utn.donatrack.donaciones.importacion.domain.ImportacionRepositorio;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Importación masiva (20.000+ filas). Estrategia:
 * <ol>
 *   <li>Lectura en streaming (nunca se carga el archivo entero en memoria).</li>
 *   <li>Lotes de {@value #TAMANIO_LOTE}: 1 consulta para detectar existentes, 1 guardado en batch,
 *       1 llamada HTTP a Autenticación. Evita el N+1 (que con 20k filas serían 60k round-trips).</li>
 *   <li>Una transacción por lote: un error no revierte lo ya importado y el progreso es visible.</li>
 *   <li>Las filas inválidas se registran con su número de línea y no frenan el proceso.</li>
 * </ol>
 */
public class ProcesarImportacion {

    public static final int TAMANIO_LOTE = 500;

    private final ImportacionRepositorio importaciones;
    private final DonanteRepositorio donantes;
    private final ServicioCuentas cuentas;
    private final AltaDeCuenta altaDeCuenta;
    private final Transaccion transaccion;
    private final Clock reloj;

    public ProcesarImportacion(ImportacionRepositorio importaciones, DonanteRepositorio donantes,
                               ServicioCuentas cuentas, AltaDeCuenta altaDeCuenta, Transaccion transaccion, Clock reloj) {
        this.importaciones = importaciones;
        this.donantes = donantes;
        this.cuentas = cuentas;
        this.altaDeCuenta = altaDeCuenta;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    public Importacion ejecutar(UUID importacionId, Iterator<String> lineas) {
        Importacion imp = importaciones.buscar(importacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Importación", importacionId));
        try {
            if (!lineas.hasNext()) {
                throw new ReglaDeNegocioException("El archivo está vacío");
            }
            FormatoCsvDonantes formato = FormatoCsvDonantes.desdeEncabezado(lineas.next());
            List<FilaDonante> lote = new ArrayList<>(TAMANIO_LOTE);
            List<Importacion.ErrorFila> errores = new ArrayList<>();
            int numero = 1;
            int leidas = 0;
            while (lineas.hasNext()) {
                String linea = lineas.next();
                numero++;
                if (linea.isBlank()) {
                    continue;
                }
                leidas++;
                try {
                    lote.add(formato.parsear(linea, numero));
                } catch (ReglaDeNegocioException e) {
                    errores.add(new Importacion.ErrorFila(numero, e.getMessage()));
                }
                if (lote.size() == TAMANIO_LOTE) {
                    procesarLote(imp, lote, errores, leidas);
                    lote = new ArrayList<>(TAMANIO_LOTE);
                    errores = new ArrayList<>();
                    leidas = 0;
                }
            }
            procesarLote(imp, lote, errores, leidas);
            imp.finalizar(reloj.instant());
        } catch (RuntimeException e) {
            imp.fallar(e.getMessage(), reloj.instant());
        }
        importaciones.guardar(imp);
        return imp;
    }

    private record Bienvenida(Destinatario destinatario, String token) {
    }

    private void procesarLote(Importacion imp, List<FilaDonante> filas, List<Importacion.ErrorFila> errores, int leidas) {
        if (filas.isEmpty()) {
            if (leidas > 0) {
                imp.registrarLote(0, 0, errores, leidas);
                importaciones.guardar(imp);
            }
            return;
        }
        List<Bienvenida> bienvenidas = new ArrayList<>();
        int[] contadores = new int[2]; // [creados, actualizados]
        transaccion.ejecutar(() -> {
            Map<String, FilaDonante> porEmail = new LinkedHashMap<>();
            filas.forEach(f -> porEmail.put(f.email(), f)); // si el email se repite en el archivo, gana la última fila
            Map<String, Donante> existentes = donantes.buscarPorEmails(porEmail.keySet()).stream()
                    .collect(Collectors.toMap(Donante::email, Function.identity()));

            List<Donante> aGuardar = new ArrayList<>();
            List<ServicioCuentas.NuevaCuenta> nuevasCuentas = new ArrayList<>();
            Map<String, Donante> porEmailGuardado = new LinkedHashMap<>();
            for (FilaDonante f : porEmail.values()) {
                try {
                    Donante d = existentes.containsKey(f.email()) ? actualizar(existentes.get(f.email()), f) : crear(f);
                    if (existentes.containsKey(f.email())) {
                        contadores[1]++;
                    } else {
                        contadores[0]++;
                    }
                    aGuardar.add(d);
                    porEmailGuardado.put(d.email(), d);
                    nuevasCuentas.add(new ServicioCuentas.NuevaCuenta(d.email(), d.nombreVisible(), "DONANTE",
                            d.tipo().name(), d.id().toString(), null));
                } catch (ReglaDeNegocioException e) {
                    errores.add(new Importacion.ErrorFila(f.numeroLinea(), e.getMessage()));
                }
            }
            donantes.guardarTodos(aGuardar);
            if (!nuevasCuentas.isEmpty()) {
                for (ServicioCuentas.CuentaCreada c : cuentas.crearEnLote(nuevasCuentas)) {
                    if (c.creada() && c.tokenActivacion() != null) {
                        bienvenidas.add(new Bienvenida(porEmailGuardado.get(c.email()).comoDestinatario(), c.tokenActivacion()));
                    }
                }
            }
        });
        // Fuera de la transacción: si el lote se revirtiera no queremos credenciales enviadas.
        bienvenidas.forEach(b -> altaDeCuenta.enviarBienvenida(b.destinatario(), b.token()));
        imp.registrarLote(contadores[0], contadores[1], errores, leidas);
        importaciones.guardar(imp);
    }

    private Donante crear(FilaDonante f) {
        Documento doc = new Documento(f.tipoDocumento(), f.documento());
        MediosContacto contacto = new MediosContacto(f.email(), f.telefono(), null, null);
        return f.tipo() == TipoDonante.HUMANA
                ? PersonaHumana.importada(UUID.randomUUID(), f.nombreDePila(), f.apellido(), doc, contacto, reloj.instant())
                : PersonaJuridica.importada(UUID.randomUUID(), f.nombre(), doc, contacto, reloj.instant());
    }

    private Donante actualizar(Donante existente, FilaDonante f) {
        if (existente.tipo() != f.tipo()) {
            throw new ReglaDeNegocioException("Línea " + f.numeroLinea() + ": el email ya pertenece a una persona "
                    + existente.tipo().name().toLowerCase());
        }
        Documento doc = new Documento(f.tipoDocumento(), f.documento());
        MediosContacto actual = existente.contacto();
        existente.actualizarContacto(new MediosContacto(actual.email(),
                f.telefono() != null ? f.telefono() : actual.telefono(), actual.whatsapp(), actual.preferido()));
        if (existente instanceof PersonaHumana h) {
            h.actualizarDatosPersonales(f.nombreDePila(), f.apellido(), null, null, doc);
        } else if (existente instanceof PersonaJuridica j) {
            j.actualizarDatos(f.nombre(), null, null, doc);
        }
        return existente;
    }
}
