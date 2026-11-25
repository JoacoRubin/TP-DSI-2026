# Matriz de requisitos → evidencia

Para cada requerimiento del enunciado (TP Anual 2026): dónde está implementado, qué prueba
automatizada lo respalda y qué pantalla lo muestra. Las pruebas se nombran `Clase.metodo`; todas
corren con `./mvnw test` (Java) y `node --test front/src/test/js` (JavaScript del front). Las capturas
están en [`docs/bocetos`](bocetos/README.md).

**Estado:** ✅ cumplido con prueba automatizada · 🟡 cumplido sin prueba automatizada o con verificación
parcial · ❌ no implementado (decisión explícita, ver
[Limitaciones](justificaciones.md#6-limitaciones-conocidas-honestidad-técnica)).

## Entrega 1: Arquitectura y modelado en objetos (I)

| # | Requerimiento | Implementación | Evidencia | Estado |
|---|---|---|---|---|
| 1 | Gestión de personas donantes | `donaciones/donantes` (humanas y jurídicas) | `DonanteTest` (7 reglas), `CasosDeUsoUsuariosTest`, `FormularioRegistroTest`; capturas 03 y 05 | ✅ |
| 2 | Gestión de donaciones resultantes | `donaciones/donaciones` (`Segmentador`, `Donacion`) | `SegmentadorTest` (ejemplos Arcos Plateados y planta de pastas), `DonacionEstadosTest`, `DonacionesApiIntegracionTest.flujoCompleto_registrarCargaSegmentarConsultarYVencer`; captura 05 | ✅ |
| 3 | Entidades beneficiarias y sus necesidades | `donaciones/beneficiarias` | `NecesidadTest` (parciales, recurrentes, baja), `AltaAntesDeCuentaRemotaJpaTest`; capturas 10 y 11 | ✅ |
| 4 | Importación masiva de donantes en CSV | `donaciones/importacion` | `FormatoCsvDonantesTest`, `ProcesarImportacionTest.veinteMilFilasSeProcesanEnLotesSinConsultasFilaPorFila`, `ImportacionesInconclusasTest`, `ProcesadorImportacionAsincronicoTest`; archivo `docs/datos/donantes-20000.csv`; captura 15 | ✅ |
| 5 | Notificaciones simuladas por distintos medios | servicio `notificaciones` | `EnviarNotificacionTest.usaElMedioPreferidoDelDestinatario`, `siElMedioPreferidoNoTieneDatosCaeAEmail`, `DonanteTest.elDestinatarioRespetaElMedioPreferido`; captura 08 | ✅ |
| 6 | Aplicación multimódulo | `pom.xml` raíz (servicios, plataforma, front, externos) | `./mvnw test` compila y prueba todos los módulos | ✅ |
| 7 | `GET` que responde "Hola desde el servicio de Donaciones." | `GET /api/saludo` | `DonacionesApiIntegracionTest.saludoPublico` | ✅ |
| E | Modelo de dominio por servicio, arquitectura, casos de uso, bocetos | `docs/modelo-dominio.md` §1-4, `docs/arquitectura.md`, `docs/diagramas/*.puml`, `docs/bocetos` | revisión documental | ✅ |

## Entrega 2: Arquitectura y modelado en objetos (II)

| # | Requerimiento | Implementación | Evidencia | Estado |
|---|---|---|---|---|
| 1 | Trazabilidad de estados de la donación | `Donacion` + historial auditado | `DonacionEstadosTest.recorridoFelizCompletoQuedaAuditado`, `noSePuedeSaltearEstados`, `unaEntregaFallidaExigeJustificacionYPuedeReplanificarse`; `docs/modelo-dominio.md` §5; captura 09 | ✅ |
| 2a | Notificación por inactividad del donante | `TareasProgramadas` (cron 09:00) | `DonanteTest.seLeAvisaUnaSolaVezPorPeriodoDeInactividad` | ✅ |
| 2b | Notificación al asignar (entidad y donante) | `ConfirmarAsignacion` | `ConfirmarAsignacionTest.confirmarAsignaComprometeSolicitaEntregaYNotificaAAmbos` | ✅ |
| 2c | Notificación al cumplir una misión | `AdaptadoresIncentivos` | `DominioIncentivosTest.lasMisionesSeCompletanEnOrdenYOtorganInsignia` prueba la misión; el envío del aviso no tiene prueba propia | 🟡 |
| 3 | Asignación asincrónica de donaciones en depósito | `TareasProgramadas` (cron 03:00) + `POST` a demanda que responde 202 | `MotorMatchmakingTest`, `AlgoritmosMatchmakingTest` (fórmulas); la ejecución asincrónica se verifica a mano | 🟡 |
| 4 | Gestión de incentivos | servicio `incentivos` (misiones, insignias, racha, ranking) | `DominioIncentivosTest` (8 casos); capturas 07 y 14 | ✅ |
| 5 | Operaciones vía API REST | controladores `*Controller` de cada servicio | `DonacionesApiIntegracionTest` (401, 403, 422 y 400 incluidos) | ✅ |
| 6 | Automatización de publicación de insignias | flujo n8n `docs/n8n/difusion-insignias.json`, `DifusorN8n` | `DifusorN8nTest`, `TextoDifusionTest`; **probado de punta a punta**: una donante nueva registra su primera donación → Incentivos le otorga "Primer paso" → n8n genera la tarjeta PNG y el post con mención ([ejemplo](n8n/ejemplo-tarjeta-insignia.png)). **No se publica en redes** (decisión del equipo). Difusión **opt-in** (Ley 25.326): sólo si la donante lo aceptó al registrarse o lo activó en su panel — `ProcesarDonacionRegistradaTest`, `ConsentimientoDifusionTest`, `ConsentimientoDifusionJpaTest`, `ResumenDonanteTest`, `PersonaHumanaRequestTest`, `FormularioRegistroTest` | ✅ |

## Entrega 3: Arquitectura y modelado en objetos (III)

| # | Requerimiento | Implementación | Evidencia | Estado |
|---|---|---|---|---|
| — | Integración con Notificaciones por cola de mensajes | Artemis embebido, `ReceptorSolicitudesNotificacion` (`@JmsListener`) | `EnviarNotificacionTest.esIdempotenteAnteMensajesDuplicados`, `siElProveedorFallaQuedaFallidaYLuegoSeReintenta` | ✅ |
| D1 | API REST de Logística | `LogisticaController` | `CasosDeUsoLogisticaTest`, `EntregasDeDonacionesTest` | ✅ |
| D2 | Planes de ruta en horario de baja carga | cron 02:00 en `ConfiguracionLogistica` | `CasosDeUsoLogisticaTest.planificar250EntregasGeneraTresLotesRespetandoElMaximoDe100` | ✅ |
| D3 | El chofer informa el comienzo de la ruta | `IniciarRuta`, sección `/chofer` | `CasosDeUsoLogisticaTest.soloElChoferDelCamionPuedeIniciarLaRutaYSeNotificanLasDonaciones` | ✅ |
| D4 | Ubicación del camión en tiempo real para donante y entidad | `POST /api/rutas/{id}/posiciones`, `/front-api/seguimiento` | `DominioLogisticaTest.ignoraPosicionesFueraDeOrdenYRechazaLasInvalidas`, `PosicionConcurrenteMongoTest`, `AccesoARutaTest`; captura 09 | ✅ |
| D5 | Recepción de la entrega en todos los casos | `ConfirmarRecepcion`, `InformarNoRecepcion`, `ReprogramarEntrega` | `CasosDeUsoLogisticaTest.laEntidadConfirmaRecepcionYLaRutaSeFinalizaAlCompletarse`, `DominioLogisticaTest.unaEntregaNoRecibidaRequiereMotivoYPuedeReprogramarse` | ✅ |
| I1 | URL de callback del planificador | `POST /api/planificaciones/callback` (secreto compartido) | `CasosDeUsoLogisticaTest.elCallbackCreaRutasYDevuelveAPendienteLasNoAsignadas` | ✅ |
| I2 | Lotes de hasta 100 donaciones | `PlanificarRutas` | `DominioLogisticaTest.lasSolicitudesAlPlanificadorSeParticionanEnLotesDeCien` | ✅ |
| I3 | Replanificar lo no asignado | callback → vuelve a pendiente | `CasosDeUsoLogisticaTest.elCallbackCreaRutasYDevuelveAPendienteLasNoAsignadas`, `siElPlanificadorFallaElLoteVuelveAPendiente`; `AlgoritmoTest` (planificador) | ✅ |
| — | Consistencia Donaciones → Logística | Transactional Outbox + entrega idempotente | `OutboxLogisticaTest`, `OutboxTransaccionalJpaTest`, `CrearEntregaIdempotenteMongoTest` | ✅ |

## Entrega 4: Persistencia y UI

| # | Requerimiento | Implementación | Evidencia | Estado |
|---|---|---|---|---|
| 1 | Persistir las entidades de cada servicio | JPA en Donaciones, Autenticación, Notificaciones e Incentivos; Spring Data Mongo en Logística | `OutboxTransaccionalJpaTest`, `AltaAntesDeCuentaRemotaJpaTest`, `DonacionesApiIntegracionTest`, los 5 tests `*MongoTest` | ✅ |
| 2 | Logística documental, el resto relacional | MongoDB (embebido 7.0.14) en Logística; H2 en modo PostgreSQL en el resto | tests `@DataMongoTest` y `@SpringBootTest` | ✅ |
| 3 | Desnormalización para lecturas | `Entrega` embebe destino y detalle; índices en `IndicesMongo` | `docs/modelo-dominio.md` §9, `ResumenPublicoMongoTest`, `BuscarEntregasMongoTest` | ✅ |
| 4 | Implementar los bocetos de la Entrega 1 | front Thymeleaf, estilos `donatrack.css` | 16 capturas reales en `docs/bocetos`, `AutorizacionPorRolTest` (navegación por rol) | ✅ |
| E | DER físico por servicio y justificación relacional | `docs/modelo-dominio.md` §8 | revisión documental | ✅ |
| — | UI/UX y accesibilidad (WCAG 2.1 AA, 48×48, responsive) | `docs/justificaciones.md` §3 | `donatrack.test.mjs` (escape de HTML, XSS); capturas | 🟡 |

## Entrega 5: Arquitectura web MVC

| # | Requerimiento | Implementación | Evidencia | Estado |
|---|---|---|---|---|
| 1 | Cliente liviano desacoplado con motor de plantillas | módulo `front` (BFF + Thymeleaf) que sólo consume APIs REST vía `ClienteApi` | `DonacionesEntidadPaginadasTest`, `MetricasPublicasTest`, `FormularioRegistroTest` (validación y cuerpo enviado a Donaciones) | ✅ |
| 2 | Servicio de Autenticación que emite los tokens front → servicios | servicio `autenticacion` (JWT), `ProveedorAutenticacionApi` | `UsuarioTest`, `CasosDeUsoUsuariosTest`, `PoliticaClavesTest`, `PoliticaAltaCuentasTest` (sin escalada a ADMIN), `DonacionesApiIntegracionTest.sinTokenNoSePuedeRegistrarDonaciones` | ✅ |
| — | Autorización por rol y CSRF en el front | `ConfiguracionSeguridadFront` | `AutorizacionPorRolTest` (60 casos: cada rol contra cada sección, anónimo al login, CSRF obligatorio, incentivos sólo para humanas, GPS sólo del chofer) | ✅ |
| B | Bonus: SSO con social login | — | no implementado | ❌ |

## Entrega 6: Despliegue, observabilidad y seguridad

| # | Requerimiento | Implementación | Evidencia | Estado |
|---|---|---|---|---|
| 1 | Despliegue en la nube con acceso público | — | **pendiente por decisión del equipo**: el sistema corre en local (`scripts/iniciar.*`) | ❌ |
| 2 | Observabilidad: métricas, trazas y logs | módulo `plataforma/observabilidad`; stack Prometheus, Tempo, Loki, Alloy y Grafana en `infraestructura-local/observabilidad` | `ValoresObservabilidadTest`, `SinObservarActuatorTest`, `DonacionesApiIntegracionTest.exponeMetricasParaPrometheusEtiquetadasConElServicio`; stack validado en vivo: 7/7 targets, 260/260 líneas de log en Loki, trazas que cruzan servicios en Tempo ([detalle](observabilidad.md#validación-en-vivo-git-bash-24112026)) | ✅ |
| 3 | Monitoreo proactivo y reinicio automático | `scripts/vigilante.*` con freno anti-bucle; alertas en `alertas.yml` | validado en Git Bash: `taskkill /F` a un servicio → el vigilante lo relanza; `detener.sh` apaga todo sin que el vigilante reviva nada; sin verificar: los scripts `.ps1` | 🟡 |
| 4 | Rate limiting | `front/limites` (Bucket4j: autenticación 10/min, GPS 60/min, general 300/min) | `LimiteSolicitudesTest`, `LimiteSolicitudesIntegracionTest.elLoginRepetidoDesdeLaMismaIpTermina429` | ✅ |
| 5 | GraphQL en Logística (filtros dinámicos y selección de campos) | `logistica.graphqls`, `EntregasGraphQl` | `EntregasGraphQlTest.filtraDinamicamenteYDevuelveSoloLosCamposPedidos`, `BuscarEntregasMongoTest` | ✅ |
| 6 | Bonus: registry + gateway | propuesta en `docs/arquitectura.md` §7 | no implementado | ❌ |
| 7 | Bonus: Transactional Outbox para eventos de notificación | outbox en Donaciones para **comandos a Logística, notificaciones y eventos a Incentivos**, y en Incentivos para **avisos de misión cumplida y categoría** (`PublicadorColasConOutbox` + `DespachadorOutbox`) | `OutboxIncentivosTest`, `OutboxIncentivosJpaTest`, `OutboxColasTest`, `OutboxLogisticaTest`, `OutboxTransaccionalJpaTest` | ✅ |
| E | Diagrama de despliegue actual | `docs/diagramas/despliegue.puml` | revisión documental | ✅ |

## Correcciones de la auditoría, cada una con su prueba de regresión

| Hallazgo | Prueba |
|---|---|
| XSS en íconos del front | `donatrack.test.mjs` |
| Cualquier usuario veía cualquier ruta | `AccesoARutaTest.usuariosAjenosNoAcceden` |
| Comando a Logística perdido o duplicado | `OutboxTransaccionalJpaTest`, `OutboxLogisticaTest`, `CrearEntregaIdempotenteMongoTest` |
| Cuenta huérfana por email duplicado | `AltaAntesDeCuentaRemotaJpaTest` |
| Entidad con más de 100 donaciones | `DonacionesEntidadPaginadasTest`, `EntregasDeDonacionesTest` |
| Cifras públicas truncadas | `ResumenPublicoMongoTest`, `MetricasPublicasTest` |
| Importación colgada en `EN_CURSO` | `ImportacionesInconclusasTest`, `ProcesadorImportacionAsincronicoTest` |
| GPS que retrocede | `PosicionConcurrenteMongoTest` |
| Servicio que crea cuentas ADMIN | `PoliticaAltaCuentasTest` |
| Sin pruebas de autorización en el front | `AutorizacionPorRolTest`, `FormularioRegistroTest` |

## Hallazgos de la validación en vivo (con Docker)

| Hallazgo | Corrección | Evidencia |
|---|---|---|
| El vigilante no se detenía y relanzaba lo que `detener.sh` apagaba | el vigilante registra su propio PID de Windows y además termina solo si desaparece su pid file | 45 s después de `detener.sh`: 0 procesos vivos |
| `--recompilar` dejaba JARs viejos si sólo cambiaba un módulo compartido | `clean package` en `iniciar.sh` / `iniciar.ps1` | los 7 JARs con la versión nueva de `observabilidad` |
| Tempo lleno de trazas de `/actuator`, Spring Security y `@Scheduled` | `ObservacionesWeb` + 2 propiedades | `SinObservarActuatorTest`, `ValoresObservabilidadTest`; en vivo, 12 de 12 trazas son acciones reales |
| **La cola de Incentivos quedaba bloqueada para siempre**: el HttpClient del JDK pide `Upgrade: h2c`, n8n no responde y la llamada no tenía timeout | HTTP/1.1 explícito + timeouts (2 s conexión, 5 s lectura) en `DifusorN8n` | `DifusorN8nTest`; en vivo, el evento trabado se re-entregó al reiniciar y se procesó |
| Post de n8n con emoji y hashtags duplicados | Incentivos manda el hecho; el formato de red social es del flujo | `TextoDifusionTest` |
| Panel del donante (7) distinto de su listado (6): cada servicio sembraba su propia historia | Historia demo única en `contratos/DatosDemo.HISTORIA` | `HistoriaDemoTest`; en vivo, 7 = 7 y 295 bienes |
| La hora del GPS del chofer decía "04:10" a las 16:10 (Chrome: `es-AR` en 12 h sin "p. m.") | `DonaTrack.hora`/`horaCorta` en 24 h y hora argentina | `donatrack.test.mjs` |
| "1 unidades", "1 misiones", "1 paradas", "lote(s)" | singular en `Donacion.descripcionCorta` y `@fmt.cantidad` | `DescripcionCortaTest`, `FormatoCantidadTest`, `MetricasPublicasTest` |
