# Justificaciones de diseño

## 1. Patrones aplicados y por qué

| Patrón | Dónde | Problema que resuelve |
|---|---|---|
| **Strategy** | `AlgoritmoMatchmaking` (Compatibilidad semántica, Prioridad a sub-atendidos) | "Contemplar la incorporación de nuevos algoritmos": agregar uno = una clase nueva, el `MotorMatchmaking` no cambia (Open/Closed). |
| **Strategy** | `CanalNotificacion` (Simulado, SMTP, Twilio SMS/WhatsApp) | Pasar de envío simulado (E1) a real (E2) cambiando **configuración**, no código. |
| **Strategy (enum)** | `TipoMision` (Racha, Completitud, Hábil donador, Donaciones exitosas) | Cada misión sabe medir su progreso; el motor de incentivos es genérico. |
| **State (máquina de estados declarativa)** | `EstadoDonacion.siguientes()`, `EstadoEntrega` | Las transiciones válidas están en **un solo lugar**: una transición ilegal es imposible por construcción y queda auditada (`CambioEstado`). |
| **Template Method** | `Necesidad` → `NecesidadExtraordinaria` / `NecesidadRecurrente` | Lógica común (comprometer, pendiente, admite donaciones) en la base; cada subtipo define cómo cuenta lo recibido (acumulado vs. por período). |
| **Domain Service** | `Segmentador`, `MotorMatchmaking`, `MotorIncentivos`, `CalculadorRanking` | Operaciones de dominio que no pertenecen a una sola entidad. |
| **Value Object** | `Direccion`, `Coordenadas`, `Documento`, `MediosContacto`, `PesosSemanticos`, `Posicion` | Validación en el constructor: un `Documento` CUIT con 3 dígitos no puede existir. |
| **Factory methods** | `PersonaHumana.registrar()` vs. `.importada()` | Dos reglas de completitud distintas (formulario exige todo; histórico CSV, lo mínimo) sin flags booleanos. |
| **Repository + Ports & Adapters** | `*Repositorio` (dominio) / `*Jpa`, `*Mongo` (infraestructura) | El dominio no conoce la base; los casos de uso se testean con fakes en memoria. |
| **Composition Root** | `ConfiguracionCasosDeUso`, `ConfiguracionLogistica`, ... | Un único lugar donde se decide qué implementación usa cada puerto. |
| **Backend for Frontend (BFF)** | `front` | El navegador nunca ve el JWT ni los servicios internos; el front agrega datos de varios servicios por pantalla. |
| **Asynchronous Request-Reply** | Importación CSV (202 + `GET /api/importaciones/{id}`), planificador (202 + callback), matchmaking a demanda | Operaciones largas sin bloquear requests HTTP. |
| **Idempotent Consumer** | Receptores JMS + casos de uso que verifican estado | El broker garantiza "al menos una vez": procesar dos veces no debe duplicar efectos. |
| **Transactional Outbox** | `ServicioLogisticaConOutbox` + `PublicadorColasConOutbox` + `DespachadorOutbox` (Donaciones; Incentivos tiene el suyo para sus avisos) | Llamar a Logística antes del commit podía dejar una entrega huérfana si después había rollback. Y publicar post-commit perdía la notificación (o el evento a Incentivos) si el proceso caía entre el commit y el envío, o si el broker estaba caído. Ahora el comando o el mensaje se confirma o se descarta JUNTO con la donación, y se reintenta hasta salir. |
| **Idempotent Receiver (por restricción)** | Índice único parcial en `entregas.donacionId` + `registrarSiNoHayActiva` | "Buscar y después insertar" no es atómico: con 8 avisos concurrentes se creaban 8 entregas (test `CrearEntregaIdempotenteMongoTest`). La base arbitra la carrera. |
| **Shared Kernel** | `plataforma/contratos` | Lenguaje común de integración (eventos, mensajes, errores base), Java puro y versionado. |

## 2. Decisiones de modelado relevantes

1. **Segmentación**: la clave de agrupación es *(subcategoría, unidad, vencimiento, estado de uso)*. La subcategoría es la unidad mínima de asignación, pero mezclar en una donación fideos que vencen en enero con fideos que vencen en marzo, o camperas nuevas con usadas, impediría evaluar y asignar correctamente. Mezclar *unidades* (kg con paquetes) haría absurdo el cálculo de cobertura del algoritmo.
2. **Peso y volumen estimados**: el enunciado da la capacidad de los camiones en kg y m³ pero no pide peso de los bienes. Sin ese dato no hay planificación posible, así que se pide como **opcional** y, si falta, se estima (`Segmentador.KG_POR_UNIDAD`). Supuesto documentado.
3. **Cantidad comprometida**: al confirmar una asignación la necesidad "reserva" la cantidad. Sin esto, dos donaciones podrían asignarse a la misma necesidad antes de entregarse y sobrecubrirla.
4. **Racha**: si el mes en curso todavía no tiene donaciones, la racha se cuenta desde el mes anterior (aún puede continuarla); si tampoco hay en el anterior, se perdió. Así se cumple "pierden progreso si no se realiza ninguna donación durante un mes completo".
5. **Hábil donador**: "una donación que supere X bienes" se mide por **carga** (lo que la persona trajo en una visita), no por donación segmentada; si no, donar 60 bienes de 3 subcategorías nunca contaría.
6. **Ranking mensual**: cantidad de misiones cumplidas en el mes (insignias con fecha dentro del período), persistido el día 1 a las 00:10 y regenerable a demanda.
7. **Importación de 20.000 filas**: streaming + lotes de 500 + una consulta y un `saveAll` por lote + alta de cuentas en lote (una llamada HTTP por lote). Las filas inválidas se registran con su número de línea y no frenan el proceso. Medido en local: **20.000 filas en ~53 s**, con 19.800 cuentas y correos de activación generados.
8. **Credenciales de cuentas importadas**: en lugar de generar y enviar contraseñas por mail (inseguro y lento: BCrypt × 20.000 ≈ 25 min), se crea la cuenta *pendiente de activación* con un token aleatorio de un solo uso y se envía el enlace.

## 3. UI/UX y accesibilidad (Entrega 4)

| Consigna | Cómo se cumple |
|---|---|
| Usabilidad / heurísticas de Nielsen | Estado del sistema visible (estados con color + texto, línea de tiempo), prevención de errores (confirmaciones con `<dialog>`, validación HTML5 + de dominio), lenguaje del usuario, ayuda contextual. |
| Profundidad de navegación ≤ 3 | Menú por rol → listado → detalle. Ej.: *Asignaciones → donación → confirmar*. |
| Patrones conocidos (Material/HIG) | Tarjetas, chips de estado, *toasts*, FAB-like primarios, tablas que se apilan en móvil. |
| Microcopy y ayudas | `placeholder`, textos de ayuda vinculados con `aria-describedby`, *tooltips* accesibles con foco de teclado, *onboarding hints* (banners). |
| Feedback no intrusivo | *Toasts* con `aria-live`; errores persistentes hasta cerrarse; banners contextuales. |
| Indicadores asincrónicos > 300 ms | *Spinner* en botones que sólo aparece pasados 300 ms; *skeletons* en mapas; páginas de progreso con *polling*. |
| Responsive | Grillas `auto-fill/minmax`, *media queries* (700 px / 900 px / 960 px), tablas apilables, menú hamburguesa con `aria-expanded`. |
| WCAG 2.1 AA | Contraste ≥ 4.5:1 (tokens verificados), foco visible, *skip link*, HTML semántico, etiquetas en todos los campos, alternativa textual a los mapas, `prefers-reduced-motion`. |
| Hit area 48×48 | Token `--tactil: 48px` aplicado a botones, enlaces de navegación, inputs y checkboxes. |
| Sistema visual unificado | Tokens de color, tipografía, espaciado (escala de 4 px), radios y sombras en `:root`. |

## 4. Estrategia de pruebas

* **Dominio** (sin Spring, sin base): segmentación con los ejemplos del enunciado (Arcos Plateados, planta de pastas), máquina de estados, necesidades parciales/recurrentes, Levenshtein y ambos algoritmos con los valores de la fórmula, motor de misiones, racha, ranking, patentes, posiciones GPS.
* **Casos de uso** con *fakes* en memoria: confirmar asignación (incluye "si Logística cae, no se notifica"), importación de 20.000 filas verificando que la cantidad de consultas sea **una por lote** (anti N+1), lotes de 100 al planificador, callback idempotente, permisos de chofer y entidad.
* **Integración**: `DonacionesApiIntegracionTest` levanta Spring + JPA (H2 en memoria) + seguridad real y prueba el flujo HTTP (401/403/422/400 incluidos).
* **Front (BFF)**: `AutorizacionPorRolTest` cruza cada rol con cada sección (403 si no corresponde, login si no hay sesión, CSRF obligatorio); `FormularioRegistroTest` valida el formulario y el cuerpo que viaja a Donaciones. Se verificó que el test falla si se afloja una regla de seguridad.
* **E2E manual automatizado** contra el sistema corriendo: login por rol, matchmaking, asignación, planificación con callback, inicio de ruta, GPS, seguimiento con filtro de privacidad, recepción con foto, comprobante, incentivos, importación de 20.000 filas, activación de cuenta.

Total: **244 tests automatizados en Java** (`./mvnw test`) y 4 de JavaScript (`node --test front/src/test/js`). Qué requisito respalda cada uno: [matriz de requisitos](matriz-requisitos.md).

## 5. Respuestas a las preguntas de discusión

### Entrega 1 — ¿Modelo de dominio rico o anémico?
Depende de dónde está la complejidad. En DonaTrack las reglas son **muchas y se cruzan** (segmentación, transiciones de estado, necesidades parciales vs. recurrentes, misiones encadenadas). Con un modelo anémico esas reglas terminan duplicadas en services, controllers y validadores, y cualquiera puede dejar una donación "Entregada" sin pasar por "En traslado". El modelo rico concentra cada invariante en un solo lugar y nos permitió escribir 90+ tests de reglas de negocio **sin levantar Spring ni una base**. El costo real (mapear dominio↔JPA) es lineal y mecánico. Para un ABM trivial (camiones) sí elegimos algo más simple (`GestionarFlota`): **la riqueza se invierte donde está la complejidad**, no por dogma.

### Entrega 2 — ¿Asignaciones en tiempo real o batch asincrónico?
Batch. El *matchmaking* compara cada donación contra todas las necesidades de su subcategoría y el segundo algoritmo depende de cuántas donaciones recibió cada entidad en el trimestre: en tiempo real, cada carga dispararía cálculos cuyo resultado cambia apenas se confirma otra asignación, y competiría por CPU/base con los usuarios en horario pico. Además **la decisión final es humana** (la administración confirma), así que la latencia de "segundos" no aporta valor. Elegimos: ejecución nocturna (03:00), ejecución a demanda **asincrónica** (202 Accepted) y recálculo puntual de una donación cuando la administración lo pide.

### Entrega 3 — ¿GPS dedicado o app móvil?
Elegimos **app móvil del chofer**. Los camiones son pocos y de una ONG: el costo de hardware + instalación + abono de datos de un GPS dedicado no se justifica cuando el chofer ya lleva un teléfono. La precisión de un smartphone moderno (~5 m) alcanza para "ver llegar el camión". Mitigamos sus debilidades en el diseño: la plataforma **valida** cada reporte (rangos, velocidad máxima, marcas de tiempo futuras), **descarta** los que llegan fuera de orden y sólo acepta posiciones de rutas en curso y del chofer de ese camión. Si la flota creciera, el contrato (`POST /api/rutas/{id}/posiciones`) es el mismo para un GPS.

### Entrega 4 — ¿NoSQL en Logística es acertado?
Sí, **por la forma de los datos y de las consultas**, no por moda. Una ruta es un agregado jerárquico (ruta → paradas → entregas) que se lee y escribe completo; el seguimiento pregunta "rutas en curso que llevan donaciones del donante X", que en documental es **una consulta sobre un campo embebido con índice**, y en relacional serían 3 JOINs. Las posiciones GPS son escrituras frecuentes y append-only: `$push` + `$slice` las acota atómicamente sin reescribir el documento. Lo que cedemos: no hay JOINs ni integridad referencial — por eso desnormalizamos los datos que necesita (destino, detalle) y **ninguna regla financiera o crítica** vive ahí.

### Entrega 5 — ¿SSR o SPA?
Para DonaTrack, **SSR**. Los usuarios son ONGs, escuelas y donantes con dispositivos y conexiones heterogéneas: SSR da primera carga rápida, funciona sin JavaScript, es más simple de hacer accesible y permite el patrón BFF (el token nunca llega al navegador). Las partes que sí necesitan interactividad (mapas en tiempo real, filas dinámicas, progreso de importación) se resolvieron con *progressive enhancement* y endpoints JSON del propio front. Una SPA tendría sentido si la app fuera un "producto" de uso intensivo con mucha interacción offline; no es el caso.

### Entrega 6 — ¿gRPC o GraphQL?
Depende del **consumidor**. gRPC brilla en comunicación servicio↔servicio de alta frecuencia con contratos estrictos (las posiciones GPS serían el candidato). GraphQL brilla cuando el consumidor es una UI que necesita filtrar y elegir campos dinámicamente (el dashboard de monitoreo y los listados de entregas). Como nuestras integraciones internas críticas ya son **asincrónicas por cola** (lo que gRPC no reemplaza), el mayor beneficio está del lado de la consulta: proponemos **GraphQL en Logística** para el dashboard administrativo.

## 6. Limitaciones conocidas (honestidad técnica)

* **ddl-auto=update** en local: en producción corresponde Flyway/Liquibase con migraciones versionadas.
* **Donaciones** (comandos a Logística, notificaciones, eventos a Incentivos) e **Incentivos** (avisos de misión cumplida y categoría) usan *Transactional Outbox*. Los **eventos de Logística** a Donaciones se publican directo, **después** de cada guardado en Mongo (`RegistrarResultadoPlanificacion` junta los `EntregaPlanificada` y los publica recién con entregas y solicitud guardadas; test `siFallaGuardarLasEntregasNoSeAnunciaNingunaPlanificacion`): nunca se anuncia algo no guardado, pero una caída justo después del guardado pierde el evento. Riesgo residual del callback: si falla al guardar las entregas, la ruta ya guardada queda huérfana y el reintento genera otra. Un outbox ahí necesita transacciones de Mongo (réplica, no disponible en el embebido).
* El código del outbox está **replicado** en Donaciones e Incentivos (entidad, bandeja, despachador). Alternativa descartada: un módulo compartido con JPA, que acoplaría a servicios sin JPA y mezclaría tablas de distintos dueños.
* El outbox es "al menos una vez": si el servicio cae entre publicar y marcar `ENVIADO`, el mensaje sale de nuevo. Los destinos lo absorben (Notificaciones descarta ids repetidos, Incentivos ignora donaciones ya procesadas).
* 20 reintentos cada 3 s (más lo que tarda en fallar la conexión: ~5-6 s por intento, medido en vivo con el broker apagado) cubren 1-2 minutos de broker o Logística caídos; si la caída dura más, el mensaje queda `FALLIDO` en `outbox_mensajes` (se loguea como ERROR). Mejora posible: *backoff* exponencial.
* El outbox asume **una sola instancia** de Donaciones despachando. Carrera aceptada: si el camión arranca y Donaciones aún no recibió `RutaIniciada`, una cancelación puede ser rechazada por Logística; queda `FALLIDO` en `outbox_mensajes` y se loguea como ERROR para revisión manual.
* El índice único parcial requiere **MongoDB ≥ 6.0** (el embebido usa 7.0.14).
* **Altas con cuenta remota** (`RegistrarPersonaHumana`, `RegistrarPersonaJuridica`, `RegistrarEntidad`): no usan outbox porque necesitan respuesta sincrónica (política de clave y token de activación). Mitigación: `guardar` hace *flush* inmediato, así cualquier error local (p. ej. email duplicado) ocurre ANTES de crear la cuenta en Autenticación (test `AltaAntesDeCuentaRemotaJpaTest`). Riesgo residual aceptado: que falle el commit mismo después de crear la cuenta (caída de la base en ese instante) deja una cuenta huérfana; se resolvería con alta idempotente en Autenticación o compensación.
* El planificador de rutas es una **simulación** (asignación first-fit + vecino más cercano), no un optimizador real.
* Los mapas usan *tiles* públicos de OpenStreetMap: requieren conexión a internet y, en producción, un proveedor con SLA.
* **Despliegue local, no en la nube**: decisión del equipo. El req. 1 de la Entrega 6 pide acceso público; queda documentado como pendiente.
* El rate limiting vive en memoria del front (una instancia). Con varias, el cupo debe compartirse (Redis / gateway).
* El reinicio automático lo hace `scripts/vigilante.*` (procesos locales). En contenedores lo haría el orquestador (`restart:` + healthcheck, o probes de Kubernetes).
