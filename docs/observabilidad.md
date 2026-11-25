# Observabilidad, monitoreo, control y GraphQL (Entrega 6)

Cómo se cumple cada requerimiento de la Entrega 6 y cómo demostrarlo en local.

| Req. | Qué hay | Dónde |
|---|---|---|
| 2. Métricas, trazas y logs | Micrometer → Prometheus; trazas OpenTelemetry (W3C `traceparent` entre servicios) → Tempo; logs JSON ECS con `traceId` → Loki; todo en Grafana | `plataforma/observabilidad`, `infraestructura-local/observabilidad` |
| 3. Monitoreo proactivo + autorestart | Reglas de alerta en Prometheus + vigilante que reinicia componentes caídos | `prometheus/alertas.yml`, `scripts/vigilante.*` |
| 4. Rate limiting | Token bucket por IP en el BFF (front), 3 políticas | `front/.../limites` |
| 5. GraphQL | Consulta de entregas con filtros dinámicos y selección de campos | `servicios/logistica/.../graphql/logistica.graphqls` |
| 1. Despliegue público | **No realizado**: el equipo decidió correr en local | `docs/justificaciones.md` §6 |

## 1. Métricas, trazas y logs

**Un solo módulo, todos los procesos.** `plataforma/observabilidad` trae las dependencias y un
`EnvironmentPostProcessor` (`ValoresObservabilidad`) con valores por defecto de menor precedencia:

* `/actuator/prometheus` expuesto, métricas etiquetadas con `application=<servicio>`.
* Muestreo de trazas 100% (local). El envío a Tempo se activa con `DONATRACK_TRAZAS=true`:
  `scripts/iniciar.*` lo hace solo si detecta Tempo en `localhost:3200`.
* Logs: la consola sigue en texto; además `logs/<servicio>.json` en formato ECS (incluye `traceId`/`spanId`).

**Trazas distribuidas.** Los `RestClient` se construyen con el `ObservationRegistry`
(front → servicios, Donaciones → Logística, Logística → planificador, Incentivos → n8n). Una
request del navegador se ve en Tempo como UNA traza que atraviesa todos los servicios.

**Sin ruido.** Al validar el stack en vivo, Tempo se llenaba de trazas que no aportan nada.
Por eso NO se observan:

| Qué | Cómo | Por qué |
|---|---|---|
| `/actuator/**` (scrapes de Prometheus, health checks del vigilante) | `ObservacionesWeb` (un `ObservationPredicate`) | 7 procesos × un scrape cada 15 s: enterraban las trazas del negocio. Tampoco cuentan en `http_server_requests`, que así mide sólo tráfico de usuarios; la vida del servicio la mide `up` |
| Spans internos de Spring Security | `management.observations.enable.spring.security=false` | 18 de los ~23 spans de cada request eran `filterchain before/after`, `authorize`... |
| Ejecuciones de `@Scheduled` | `management.observations.enable.tasks.scheduled=false` | El despacho del outbox corre cada 3 s: una traza vacía por ejecución. Si despacha algo, la llamada a Logística igual se traza |

Resultado medido: entrar a `/donante` es una traza de 5 spans (front → Donaciones + Incentivos)
que muestra de un vistazo que el 60 % del tiempo es la consulta de métricas a Incentivos.

**Etiquetas en Loki:** `servicio` y `nivel` (p. ej. `{servicio="front", nivel="ERROR"}`).

### Levantar el stack (requiere Docker)

```bash
docker compose -f infraestructura-local/observabilidad/docker-compose.yml up -d
./scripts/iniciar.sh          # o .\scripts\iniciar.ps1 (detecta Tempo y activa las trazas)
```

| Herramienta | URL | Para qué |
|---|---|---|
| Grafana | http://localhost:3000 | Dashboard **DonaTrack - Estado de los servicios**; Explore para trazas (Tempo) y logs (Loki) |
| Prometheus | http://localhost:9090 | Targets (`Status → Targets`) y alertas (`Alerts`) |

**Navegación log → traza:** en un log de Loki, el campo `traceId` es un enlace a la traza en Tempo;
desde una traza, "Logs for this span" vuelve a los logs de esa misma request.

## 2. Monitoreo proactivo y reinicio automático

**Detectar** (Prometheus, `alertas.yml`): `ServicioCaido` (sin respuesta 30 s), `TasaDeErrores5xxAlta`
(> 5%), `LatenciaP95Alta` (> 2 s), `MemoriaHeapAlta` (> 90%), `RateLimitingActivo`.

**Actuar** (`scripts/vigilante.ps1` / `.sh`, lo lanza `iniciar` salvo `-SinVigilante` / `--sin-vigilante`):

* Cada 15 s: ¿vive el proceso? ¿`/actuator/health` responde 200?
* Reinicia si el proceso murió o tras 3 health checks fallidos seguidos.
* Gracia tras (re)iniciar: no evalúa hasta pasado el tiempo de arranque del componente.
* **Freno anti-bucle**: 5 reinicios en 10 min → deja de reiniciar ese componente y registra ERROR
  (un error de configuración no se arregla reiniciando; ocultarlo sería peor).
* Cada evento queda en `logs/vigilante.json` → visible en Grafana. El log del proceso caído se
  conserva en `logs/<componente>.anterior.log`.

**Demo:** con todo corriendo, matar un servicio (p. ej. el PID de `logs/pids/incentivos.pid`).
En ≤ 30 s Prometheus marca `ServicioCaido`, el vigilante lo relanza y el panel "Servicios arriba" vuelve a verde.

**Apagado seguro:** `detener` mata primero al vigilante. En Git Bash el vigilante registra su
propio PID de Windows (`$$` → `/proc/$$/winpid`): el `$!` que ve `iniciar.sh` es la etapa `nohup`,
que MSYS reemplaza al hacer `exec`, y con ese PID `taskkill` no lo encontraba y el vigilante
relanzaba todo lo que se acababa de detener. Segunda barrera: si `logs/pids/vigilante.pid`
desaparece, el vigilante termina solo antes de relanzar cualquier cosa.

**PID real y puerto libre (25/11/2026):** una vez `detener.sh` borró los `.pid` del front y de Incentivos
sin matarlos (el PID registrado desde Git Bash no era el del `java.exe`) y quedaron escuchando. Ahora:
* El PID confiable es el que el servicio anota en su log (`Started ... with PID N`). `iniciar.sh` lo
  registra tras el health OK, y el vigilante corrige un `.pid` equivocado en lugar de "reiniciar" un servicio sano.
* `detener.sh` recorre `componentes.csv` y verifica que cada **puerto** quede libre. Si sigue ocupado
  por NUESTRO proceso (PID del log == PID que escucha en el puerto), lo mata. A un proceso ajeno no
  lo toca: avisa, conserva el `.pid` y sale con 1. El vigilante tampoco relanza sobre un puerto ocupado.

### Validación en vivo (Git Bash, 24/11/2026)

| Qué | Resultado |
|---|---|
| Stack: Prometheus, Loki, Tempo, Grafana | `/ready` 200 en los cuatro; Alloy siguiendo los 7 `logs/*.json` |
| Métricas | 7/7 targets `up`; 5 reglas de alerta cargadas e inactivas |
| Logs | Loki recibió las 260 líneas de los 260 escritas, por servicio y nivel |
| Trazas | `GET /` = una traza front → Logística (`/resumen`, `/entregas`) + Incentivos (`/podio`) |
| Detección del stack | `iniciar.sh`: "Stack de observabilidad detectado: trazas activadas" |
| Reinicio automático | `taskkill /F` a Incentivos → el vigilante lo relanzó con PID nuevo y health 200 |
| Apagado | tras `detener.sh`, 45 s sin ningún `java.exe` ni vigilante vivos |
| `.pid` equivocado (25/11) | PID falso en `front.pid`: el vigilante lo corrigió al PID real sin reiniciar · PID falsos en front e Incentivos: `detener.sh` los detuvo igual, 9/9 puertos libres · servidor ajeno (`jwebserver`) en 8090: `detener.sh` no lo tocó, avisó y salió con 1 |
| Sin verificar | `iniciar.ps1`, `detener.ps1` y `vigilante.ps1` (PowerShell) |

## 3. Rate limiting

Vive en el **front** porque es el BFF: todo el tráfico del navegador entra por ahí (equivale a
limitar en un API Gateway). Corre **antes** de Spring Security: la fuerza bruta se corta antes de verificar claves.

| Política | Aplica a | Cupo por IP |
|---|---|---|
| `AUTENTICACION` | `POST /login`, `/registro/**`, `/activar` | 10 / min |
| `GPS` | `POST /front-api/chofer/rutas/*/posiciones` | 60 / min |
| `GENERAL` | resto (estáticos y `/actuator` excluidos) | 300 / min |

Al exceder: `429 Too Many Requests` + `Retry-After`. Métrica `donatrack_limite_rechazos_total{politica}`.
Limitaciones: cupo en memoria (una instancia); detrás de un proxy habría que usar `X-Forwarded-For` sólo si es confiable.

**Demo:** `for i in $(seq 12); do curl -s -o /dev/null -w "%{http_code}\n" -X POST localhost:8080/login; done` → los últimos dan 429.

## 4. GraphQL en Logística

`POST http://localhost:8082/graphql` con `Authorization: Bearer <token de ADMIN>`. Explorador en `/graphiql` (perfil local).

```graphql
query {
  entregas(filtro: { estados: [PENDIENTE, PLANIFICADA], texto: "sillas",
                     desde: "2026-11-01T00:00:00Z" }, pagina: 0, tamanio: 20) {
    total
    elementos { entidadNombre descripcion estado patente }
  }
}
```

* Filtros opcionales y combinables (AND): estados, entidad, donante, patente, rango de fechas, texto libre.
* El cliente elige los campos: el dashboard pide 4 campos en lugar de los 24 de la API REST.
* `total` es el total real (consulta `count` en Mongo), no el tamaño de la página. Máximo 100 por página.
* El texto se busca literal (`Pattern.quote`): no hay inyección de expresiones regulares.

**¿Por qué GraphQL y no gRPC?** (pregunta de discusión) Depende del consumidor. gRPC rinde en
servicio↔servicio de alta frecuencia con contratos estrictos, pero nuestras integraciones internas
críticas ya son asincrónicas por cola. El consumidor que necesitaba flexibilidad era la UI administrativa:
filtros que cambian según lo que el operador mira y campos distintos por pantalla. Ahí GraphQL evita
multiplicar endpoints REST a medida.

## Tests que respaldan esto

`ValoresObservabilidadTest`, `SinObservarActuatorTest`, `DonacionesApiIntegracionTest#exponeMetricasParaPrometheus...`,
`LimiteSolicitudesTest`, `LimiteSolicitudesIntegracionTest`, `BuscarEntregasMongoTest`, `EntregasGraphQlTest`.
