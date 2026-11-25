# Modelo de dominio y modelos de datos

> Diagramas en Mermaid: se renderizan en GitHub/GitLab, VS Code (extensión *Markdown Preview Mermaid*) o en <https://mermaid.live>.
> Los diagramas UML de **casos de uso** y **despliegue** están en PlantUML: [`diagramas/casos-de-uso.puml`](diagramas/casos-de-uso.puml) y [`diagramas/despliegue.puml`](diagramas/despliegue.puml).

---

## 1. Servicio de Donaciones

```mermaid
classDiagram
direction LR
class Donante {
  <<abstract>>
  UUID id
  MediosContacto contacto
  Direccion direccion
  boolean activo
  Instant ultimaInteraccion
  Instant ultimoAvisoInactividad
  +nombreVisible()* String
  +documento()* Documento
  +comoDestinatario() Destinatario
  +registrarInteraccion(Instant)
  +requiereAvisoInactividad(Instant, Duration) boolean
}
class PersonaHumana {
  String nombre
  String apellido
  Integer edad
  Genero genero
  boolean aceptaDifusionLogros
  +registrar(...)$ PersonaHumana
  +importada(...)$ PersonaHumana
  +aceptarDifusionLogros(boolean)
}
class PersonaJuridica {
  String razonSocial
  TipoPersonaJuridica tipoOrganizacion
  String rubro
  List~Representante~ representantes
}
class MediosContacto {
  <<value object>>
  String email
  String telefono
  String whatsapp
  MedioNotificacion preferido
}
class Documento { <<value object>> TipoDocumento tipo; String numero }
class Direccion { <<value object>> calle, altura, localidad, provincia, codigoPostal }
class Coordenadas { <<value object>> double latitud; double longitud }
Donante <|-- PersonaHumana
Donante <|-- PersonaJuridica
Donante *-- MediosContacto
Donante *-- Direccion
PersonaJuridica *-- "0..*" Representante
Direccion *-- "0..1" Coordenadas

class Categoria { UUID id; String nombre; boolean perecedera; boolean requiereEstadoUso }
class Subcategoria { <<value object>> UUID id; String nombre }
Categoria *-- "1..*" Subcategoria

class CargaDonacion { <<value object>> UUID donanteId; String descripcionGeneral; List~BienDeclarado~ bienes }
class Segmentador {
  <<domain service>>
  +segmentar(CargaDonacion, ...) List~Donacion~
}
class Donacion {
  <<aggregate root>>
  UUID id
  UUID cargaId
  UUID donanteId
  UUID subcategoriaId
  String unidad
  LocalDate fechaVencimiento
  EstadoUso estadoUso
  double pesoKg
  double volumenM3
  EstadoDonacion estado
  UUID necesidadId
  UUID entidadId
  +asignarA(necesidad, entidad, responsable, Instant)
  +marcarListaParaEntregar(Instant)
  +iniciarTraslado(patente, Instant)
  +confirmarEntrega(patente, Instant)
  +registrarEntregaFallida(motivo, Instant)
  +devolverADeposito(responsable, motivo, Instant)
  +marcarVencida(responsable, motivo, Instant)
  +cantidadTotal() int
}
class Bien { <<value object>> String descripcion; int cantidad; String fotoUrl }
class CambioEstado { <<value object>> desde; hasta; fecha; responsable; motivo }
class EstadoDonacion {
  <<enumeration>>
  EN_DEPOSITO
  ASIGNADA
  LISTA_PARA_ENTREGAR
  EN_TRASLADO
  ENTREGADA
  ENTREGA_FALLIDA
  VENCIDA
  +siguientes() Set
}
Segmentador ..> CargaDonacion
Segmentador ..> Donacion : crea
Donacion *-- "1..*" Bien
Donacion *-- "1..*" CambioEstado : historial (auditoría)
Donacion --> EstadoDonacion
Donacion ..> Subcategoria : subcategoriaId

class EntidadBeneficiaria { UUID id; String razonSocial; Direccion direccion; String telefono; List~String~ emails }
class Necesidad {
  <<abstract>>
  UUID id
  UUID entidadId
  UUID subcategoriaId
  String descripcion
  int cantidadRequerida
  int cantidadComprometida
  +tipo()* TipoNecesidad
  +cantidadRecibida(LocalDate)* int
  +cantidadPendiente(LocalDate) int
  +admiteDonaciones(LocalDate) boolean
  +comprometer(int)
  +registrarRecepcion(int, LocalDate)
}
class NecesidadExtraordinaria { int cantidadRecibida }
class NecesidadRecurrente { Periodicidad periodicidad; List~Recepcion~ recepciones }
Necesidad <|-- NecesidadExtraordinaria
Necesidad <|-- NecesidadRecurrente
EntidadBeneficiaria "1" --> "0..*" Necesidad : entidadId

class AlgoritmoMatchmaking {
  <<interface>>
  +nombre() String
  +puntuar(Donacion, Necesidad, ContextoMatchmaking) double
}
class CompatibilidadSemantica { Supplier~PesosSemanticos~ pesos }
class PrioridadSubAtendidos { double k }
class MotorMatchmaking {
  +TOP = 10
  +evaluar(Donacion, List~Necesidad~, Contexto, Instant) ResultadoMatchmaking
}
class ResultadoMatchmaking { UUID donacionId; List~Ranking~ rankings; List~UUID~ coincidencias }
class PesosSemanticos { <<value object>> textual; volumen; tipo }
AlgoritmoMatchmaking <|.. CompatibilidadSemantica
AlgoritmoMatchmaking <|.. PrioridadSubAtendidos
MotorMatchmaking o-- "1..*" AlgoritmoMatchmaking
MotorMatchmaking ..> ResultadoMatchmaking
CompatibilidadSemantica ..> PesosSemanticos

class Importacion { UUID id; Estado estado; int creados; int actualizados; int conError; List~ErrorFila~ errores }
class FormatoCsvDonantes { +desdeEncabezado(String)$; +parsear(String, int) FilaDonante }
```

## 2. Servicio de Logística (persistencia documental)

```mermaid
classDiagram
direction LR
class Camion { UUID id; String patente; double volumenM3; double alturaM; double cargaKg; boolean activo }
class Entrega {
  <<aggregate root>>
  UUID id
  UUID donacionId
  UUID donanteId
  UUID entidadId
  Destino destino
  DetalleDonacion detalle
  double pesoKg
  double volumenM3
  EstadoEntrega estado
  UUID rutaId
  String patente
  List~String~ fotos
  +enviarAPlanificar(Instant)
  +planificar(rutaId, Camion, Instant)
  +iniciarTraslado(Instant)
  +confirmarRecepcion(fotos, Instant)
  +informarNoRecibida(motivo, Instant)
  +reprogramar(Instant)
}
class Destino { <<value object, desnormalizado>> entidadNombre; direccion; latitud; longitud }
class DetalleDonacion { <<value object, desnormalizado>> descripcion; subcategoria; cantidad; unidad }
class Ruta {
  <<aggregate root>>
  UUID id
  LocalDate fecha
  String patente
  EstadoRuta estado
  Posicion ultimaPosicion
  List~Posicion~ recorrido (≤300)
  +iniciar(Instant)
  +registrarPosicion(Posicion, Instant) boolean
  +involucraDonante(UUID) boolean
}
class Parada { <<embebido>> int orden; UUID entidadId; Destino destino; List~EntregaEnRuta~ entregas }
class EntregaEnRuta { <<embebido>> entregaId; donacionId; donanteId; descripcion }
class Posicion { <<value object>> latitud; longitud; velocidadKmh; registradaEn }
class SolicitudPlanificacion { UUID id; List~UUID~ entregaIds (≤100); Estado estado; int sinAsignar }
class PlanificadorRutas { <<puerto externo>> +solicitar(Pedido) }
Entrega *-- Destino
Entrega *-- DetalleDonacion
Ruta *-- "1..*" Parada
Parada *-- "1..*" EntregaEnRuta
Ruta *-- Posicion
Ruta ..> Camion : camionId
Entrega ..> Ruta : rutaId
SolicitudPlanificacion ..> Entrega : entregaIds
```

## 3. Servicio de Incentivos

```mermaid
classDiagram
direction LR
class PerfilDonante { UUID donanteId; String nombre; CategoriaDonante categoria; int indiceMision; boolean insigniasVisibles; +misionActual(Catalogo) }
class CategoriaDonante { <<enumeration>> COLABORADOR; SOSTENEDOR; TRANSFORMADOR; +siguiente() }
class Mision { <<value object>> String id; CategoriaDonante categoria; int orden; TipoMision tipo; int objetivo; String insignia }
class TipoMision {
  <<enumeration / strategy>>
  RACHA
  COMPLETITUD
  HABIL_DONADOR
  DONACIONES_EXITOSAS
  +progreso(HistorialDonante, YearMonth)* int
}
class CatalogoMisiones { List~Mision~ misiones; +en(categoria, indice) }
class MotorIncentivos { <<domain service>> +evaluar(perfil, historial, mes, Instant) Resultado; +progresoActual(...) }
class HistorialDonante { <<value object>> +rachaMeses(YearMonth); +categoriasDistintas(); +mayorCarga(); +donacionesExitosas() }
class ActividadDonacion { UUID donacionId; UUID cargaId; String categoria; int cantidad; boolean entregada; UUID entidadId }
class InsigniaObtenida { UUID id; UUID donanteId; String misionId; Instant obtenidaEn }
class RankingMensual { String id (yyyy-MM); List~Entrada~ entradas; +podio() }
class CalculadorRanking { +calcular(periodo, insignias, nombres, Instant)$ RankingMensual }
PerfilDonante --> CategoriaDonante
CatalogoMisiones *-- "1..*" Mision
Mision --> TipoMision
MotorIncentivos --> CatalogoMisiones
MotorIncentivos ..> HistorialDonante
HistorialDonante o-- "0..*" ActividadDonacion
MotorIncentivos ..> InsigniaObtenida : otorga
CalculadorRanking ..> RankingMensual
```

## 4. Servicios de Notificaciones y Autenticación

```mermaid
classDiagram
direction LR
class Notificacion { UUID id; Destinatario destinatario; String asunto; String mensaje; EstadoNotificacion estado; int intentos; +intentarEnvio(SelectorCanal, Instant) }
class CanalNotificacion { <<interface / strategy>> +medio() MedioNotificacion; +enviar(Destinatario, asunto, mensaje) }
class SelectorCanal { +elegir(Destinatario, MedioNotificacion) CanalNotificacion }
class CanalSimulado
class CanalEmailSmtp
class CanalTwilio
CanalNotificacion <|.. CanalSimulado
CanalNotificacion <|.. CanalEmailSmtp
CanalNotificacion <|.. CanalTwilio
SelectorCanal o-- "1..*" CanalNotificacion
Notificacion ..> SelectorCanal

class Usuario { UUID id; String email; Rol rol; String perfil; String referencia; String claveHash; EstadoUsuario estado; String tokenActivacionHash; +activar(hash, claveHash, Instant); +verificarHabilitadoParaIngresar() }
class Rol { <<enumeration>> DONANTE; BENEFICIARIA; ADMIN; CHOFER }
class PoliticaClaves { +validar(String)$ }
Usuario --> Rol
```

---

## 5. Estados de una donación (trazabilidad)

```mermaid
stateDiagram-v2
    [*] --> EN_DEPOSITO : registrar carga (segmentación)
    EN_DEPOSITO --> ASIGNADA : admin confirma entidad\n(top-10 de los algoritmos)
    EN_DEPOSITO --> VENCIDA : admin marca vencida
    ASIGNADA --> LISTA_PARA_ENTREGAR : evento EntregaPlanificada
    ASIGNADA --> EN_DEPOSITO : admin deshace asignación
    ASIGNADA --> VENCIDA
    LISTA_PARA_ENTREGAR --> EN_TRASLADO : evento RutaIniciada
    LISTA_PARA_ENTREGAR --> VENCIDA
    EN_TRASLADO --> ENTREGADA : entidad confirma recepción
    EN_TRASLADO --> ENTREGA_FALLIDA : no recibida (con justificación)
    ENTREGA_FALLIDA --> LISTA_PARA_ENTREGAR : replanificada
    ENTREGA_FALLIDA --> EN_DEPOSITO : vuelve al depósito para reasignar
    ENTREGA_FALLIDA --> VENCIDA
    ENTREGADA --> [*]
    VENCIDA --> [*]
```

## 6. Estados de una entrega (Logística)

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE : Donaciones solicita la entrega
    PENDIENTE --> EN_PLANIFICACION : enviada al planificador (lote ≤ 100)
    EN_PLANIFICACION --> PLANIFICADA : callback: incluida en una ruta
    EN_PLANIFICACION --> PENDIENTE : callback: sin asignar / planificador caído
    PLANIFICADA --> EN_TRASLADO : chofer inicia la ruta
    EN_TRASLADO --> ENTREGADA : entidad confirma (+ fotos)
    EN_TRASLADO --> NO_RECIBIDA : entidad o chofer informan
    NO_RECIBIDA --> PENDIENTE : administración reprograma
    PENDIENTE --> CANCELADA : donación vencida / reasignada
    PLANIFICADA --> CANCELADA
    NO_RECIBIDA --> CANCELADA
```

## 7. Secuencia del flujo principal (asignación → entrega)

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant F as Front (BFF)
    participant D as Donaciones
    participant L as Logística
    participant P as Planificador externo
    participant B as Broker (JMS)
    participant N as Notificaciones
    participant I as Incentivos
    actor Chofer
    actor Entidad

    Admin->>F: Confirmar entidad para la donación
    F->>D: POST /api/donaciones/{id}/asignacion (JWT usuario)
    D->>D: Donacion.asignarA() · Necesidad.comprometer()
    D->>L: POST /api/entregas (token de servicio)
    D-->>B: SolicitudNotificacion ×2 (post-commit)
    B-->>N: entidad y donante notificados
    Note over L,P: 02:00 (o a demanda)
    L->>P: POST /api/planificaciones (lote ≤ 100, callbackUrl)
    P-->>L: 202 Accepted
    P->>L: POST callback (rutas, sinAsignar) + X-Callback-Token
    L-->>B: EntregaPlanificada
    B-->>D: Donación → LISTA_PARA_ENTREGAR
    Chofer->>F: Iniciar ruta
    F->>L: POST /api/rutas/{id}/inicio
    L-->>B: RutaIniciada (+ enlace al mapa)
    B-->>D: Donación → EN_TRASLADO · notifica con enlace
    loop cada pocos segundos
        Chofer->>F: posición GPS
        F->>L: POST /api/rutas/{id}/posiciones
    end
    Entidad->>F: Confirmar recepción + fotos
    F->>L: POST /api/entregas/{id}/recepcion (multipart)
    L-->>B: EntregaRealizada
    B-->>D: Donación → ENTREGADA · Necesidad.registrarRecepcion()
    D-->>B: comprobante (fecha, hora, camión) + DonacionEntregada
    B-->>I: progreso de misiones → insignias → n8n
```

---

## 8. Modelo de datos relacional (DER físico por servicio)

Cada servicio tiene **su propio esquema** (en local: un archivo H2 por servicio; en producción: un esquema PostgreSQL por servicio sobre el mismo motor, perfil `postgres`). No hay claves foráneas **entre** servicios: las referencias cruzadas son UUID (consistencia eventual vía eventos).

### 8.1 Donaciones

```mermaid
erDiagram
    DONANTES ||--o{ DONANTES_REPRESENTANTES : tiene
    DONANTES {
        uuid id PK
        varchar tipo "HUMANA | JURIDICA (discriminador)"
        varchar email UK
        varchar telefono
        varchar whatsapp
        varchar medio_preferido
        varchar nombre_visible "desnormalizado: búsqueda/orden"
        varchar tipo_documento
        varchar numero_documento
        varchar nombre "sólo HUMANA"
        varchar apellido "sólo HUMANA"
        int edad "sólo HUMANA"
        varchar genero "sólo HUMANA"
        boolean acepta_difusion_logros "sólo HUMANA; opt-in, NULL = no"
        varchar razon_social "sólo JURIDICA"
        varchar tipo_organizacion "sólo JURIDICA"
        varchar rubro "sólo JURIDICA"
        varchar calle
        varchar localidad
        double latitud
        double longitud
        boolean activo
        timestamp ultima_interaccion "índice compuesto con activo"
        timestamp ultimo_aviso_inactividad
    }
    DONANTES_REPRESENTANTES { uuid donante_id FK; varchar nombre; varchar email }

    CATEGORIAS ||--|{ SUBCATEGORIAS : contiene
    CATEGORIAS { uuid id PK; varchar nombre; boolean perecedera; boolean requiere_estado_uso }
    SUBCATEGORIAS { uuid categoria_id FK; uuid subcategoria_id; varchar nombre }

    DONACIONES ||--|{ DONACIONES_BIENES : contiene
    DONACIONES ||--|{ DONACIONES_HISTORIAL : audita
    DONACIONES {
        uuid id PK
        uuid carga_id "agrupa la carga única"
        uuid donante_id "ref. lógica"
        uuid subcategoria_id
        varchar subcategoria_nombre "desnormalizado"
        varchar categoria_nombre "desnormalizado: filtro sin JOIN"
        int cantidad_total "desnormalizado"
        varchar unidad
        date fecha_vencimiento
        varchar estado_uso
        double peso_kg
        double volumen_m3
        varchar estado "índice"
        uuid necesidad_id
        uuid entidad_id "índice (entidad, estado)"
        varchar patente_entrega
        timestamp entregada_en "índice (estado, entregada_en)"
        timestamp registrada_en
    }
    DONACIONES_BIENES { uuid donacion_id FK; int orden; varchar descripcion; int cantidad; varchar foto_url }
    DONACIONES_HISTORIAL { uuid donacion_id FK; int orden; varchar desde; varchar hasta; timestamp fecha; varchar responsable; varchar motivo }

    ENTIDADES_BENEFICIARIAS ||--|{ ENTIDADES_EMAILS : tiene
    ENTIDADES_BENEFICIARIAS { uuid id PK; varchar razon_social; varchar email_principal UK; varchar telefono; varchar calle; double latitud; double longitud; boolean activa }
    ENTIDADES_EMAILS { uuid entidad_id FK; int orden; varchar email }

    NECESIDADES ||--o{ NECESIDADES_RECEPCIONES : registra
    NECESIDADES {
        uuid id PK
        varchar tipo "EXTRAORDINARIA | RECURRENTE"
        uuid entidad_id "índice"
        uuid subcategoria_id "índice (subcategoria, activa)"
        varchar descripcion
        int cantidad_requerida
        int cantidad_comprometida
        int cantidad_recibida "sólo EXTRAORDINARIA"
        varchar periodicidad "sólo RECURRENTE"
        boolean activa
    }
    NECESIDADES_RECEPCIONES { uuid necesidad_id FK; date fecha; int cantidad }

    RESULTADOS_MATCHMAKING ||--o{ RESULTADOS_PUNTAJES : contiene
    RESULTADOS_MATCHMAKING ||--o{ RESULTADOS_COINCIDENCIAS : contiene
    RESULTADOS_MATCHMAKING { uuid donacion_id PK; timestamp ejecutado_en }
    RESULTADOS_PUNTAJES { uuid donacion_id FK; int orden; varchar algoritmo; int posicion; uuid necesidad_id; double valor }
    RESULTADOS_COINCIDENCIAS { uuid donacion_id FK; int orden; uuid necesidad_id }
    CONFIGURACION_MATCHMAKING { varchar clave PK; double peso_textual; double peso_volumen; double peso_tipo }

    IMPORTACIONES ||--o{ IMPORTACIONES_ERRORES : reporta
    IMPORTACIONES { uuid id PK; varchar archivo; varchar estado; int filas_procesadas; int creados; int actualizados; int con_error }
    IMPORTACIONES_ERRORES { uuid importacion_id FK; int linea; varchar mensaje }
```

### 8.2 Incentivos, Notificaciones y Autenticación

```mermaid
erDiagram
    PERFILES_DONANTES { uuid donante_id PK; varchar nombre; varchar email; varchar categoria; int indice_mision; boolean insignias_visibles }
    ACTIVIDAD_DONACIONES { uuid donacion_id PK; uuid donante_id "índice (donante, fecha)"; uuid carga_id; varchar categoria; int cantidad; timestamp registrada_en; boolean entregada; uuid entidad_id }
    INSIGNIAS { uuid id PK; uuid donante_id "índice"; varchar mision_id; varchar nombre; timestamp obtenida_en "índice" }
    RANKINGS_MENSUALES ||--|{ RANKINGS_ENTRADAS : contiene
    RANKINGS_MENSUALES { varchar id PK "yyyy-MM"; int anio; int mes; timestamp generado_en }
    RANKINGS_ENTRADAS { varchar ranking_id FK; int orden; int posicion; uuid donante_id; varchar nombre "desnormalizado"; int misiones_cumplidas }

    NOTIFICACIONES { uuid id PK "idempotencia"; varchar destinatario_email "índice (email, fecha)"; varchar medio_utilizado; varchar asunto; varchar mensaje; varchar estado "índice"; int intentos; boolean leida }

    USUARIOS { uuid id PK; varchar email UK; varchar rol; varchar perfil; varchar referencia; varchar clave_hash "BCrypt"; varchar estado; varchar token_activacion_hash "SHA-256, índice" }
```

## 9. Modelo documental (Logística · MongoDB)

```text
camiones                     { _id: UUID, patente (único), volumenM3, alturaM, cargaKg, activo }

entregas                     { _id, donacionId (índice), donanteId, donanteNombre, entidadId (índice),
                               destino: { entidadNombre, direccion, latitud, longitud },     ← embebido / desnormalizado
                               detalle: { descripcion, subcategoria, cantidad, unidad },     ← embebido / desnormalizado
                               pesoKg, volumenM3, estado (índice con creadaEn), rutaId (índice),
                               camionId, patente, fotos: [url], motivoNoRecepcion, entregadaEn,
                               historial: [ { estado, fecha, detalle } ] }

rutas                        { _id, fecha, camionId, patente, estado,
                               paradas: [ { orden, entidadId, destino: {...},
                                            entregas: [ { entregaId, donacionId, donanteId, descripcion } ] } ],
                               ultimaPosicion: { latitud, longitud, velocidadKmh, registradaEn },
                               recorrido: [ ...máx. 300 posiciones ($push + $slice) ] }
                             índices: (estado, paradas.entregas.donanteId), (estado, paradas.entidadId), (patente, estado)

solicitudes_planificacion    { _id, fechaRutas, entregaIds: [≤ 100], estado, enviadaEn, completadaEn, rutasGeneradas, sinAsignar }
```
