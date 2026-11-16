# DonaTrack — Sistema de Gestión y Trazabilidad de Donaciones

Trabajo Práctico Anual Integrador · Diseño de Sistemas de Información · UTN FRBA · 2026

DonaTrack organiza, registra y sigue las donaciones de **UTN Solidaria** desde que ingresan al depósito hasta que una
entidad beneficiaria las recibe: segmentación automática por subcategoría, asignación con algoritmos de *matchmaking*,
planificación de rutas, seguimiento de camiones en tiempo real, notificaciones por email/SMS/WhatsApp e incentivos con
misiones, insignias y ranking.

| Documento | Contenido |
|---|---|
| [`docs/arquitectura.md`](docs/arquitectura.md) | Análisis de estilos (monolito, monolito modular, servicios, microservicios) y decisión; componentes, mensajería, seguridad, evolución |
| [`docs/modelo-dominio.md`](docs/modelo-dominio.md) | Diagramas de clases por servicio, estados, secuencia, DER físico por servicio y modelo documental |
| [`docs/justificaciones.md`](docs/justificaciones.md) | Patrones, decisiones de modelado, UI/UX/WCAG, pruebas y **respuestas a las 6 preguntas de discusión** |
| [`docs/diagramas/`](docs/diagramas) | Casos de uso y despliegue (PlantUML) |
| [`docs/n8n/difusion-insignias.json`](docs/n8n/difusion-insignias.json) | Flujo low-code de difusión de insignias (importable en n8n) |

---

## Cómo correrlo en local

**Requisito único: JDK 21.** No hace falta instalar Maven, MongoDB, PostgreSQL ni un broker: el proyecto trae Maven
Wrapper, H2 en archivo, MongoDB embebido y un broker Artemis propio.

```powershell
# Windows (PowerShell)
.\scripts\iniciar.ps1          # compila la primera vez y levanta los 8 procesos
.\scripts\detener.ps1
```

```bash
# Linux / macOS / Git Bash
./scripts/iniciar.sh
./scripts/detener.sh
```

Abrí **<http://localhost:8080>**. La primera ejecución descarga dependencias y el binario de MongoDB (~1–3 min).
Los datos persisten en `./data/` y los logs quedan en `./logs/`. Para empezar de cero: detener y borrar `./data/`.

### Cuentas de demostración (contraseña `Demo1234!`)

| Rol | Email | Para probar |
|---|---|---|
| Administración | `admin@donatrack.org` | Registrar donaciones, asignar, planificar rutas, monitoreo, ranking, importar CSV |
| Donante (persona humana) | `ana@donatrack.org` | Mis donaciones, seguimiento, **incentivos** |
| Donante (persona jurídica) | `contacto@arcosplateados.com` | Donaciones de la oficina en mudanza |
| Entidad beneficiaria | `escuela10@donatrack.org` · `comedor@donatrack.org` · `hogar@donatrack.org` | Necesidades, confirmar recepción con fotos |
| Chofer (camión `AE123BC`) | `chofer@donatrack.org` | Iniciar ruta y reportar ubicación (GPS real o simulado) |

### Recorrido sugerido de punta a punta (≈ 5 minutos)

1. **admin** → *Asignaciones* → *Ejecutar algoritmos ahora* → abrir "6 unidades de Sillas" → elegir la entidad (verás las coincidencias de ambos algoritmos).
2. **admin** → *Logística* → *Planificar rutas ahora* (el planificador externo responde por callback en ~2 s).
3. **chofer** → *Mis rutas* → abrir la ruta → *Iniciar recorrido* → *Simular recorrido*.
4. **contacto@arcosplateados.com** → *Seguimiento*: el camión se mueve en el mapa (se actualiza cada 10 s).
5. **comedor@donatrack.org** (o la entidad elegida) → *Donaciones recibidas* → *Confirmar recepción* con una foto.
6. Mirá las *Notificaciones* de cada cuenta (incluye el comprobante con fecha, hora y camión), la *Galería* pública y el *Mapa*.
7. **admin** → *Importar* → subí `docs/datos/donantes-20000.csv` (20.000 filas, ~1 min). Generalo con `./scripts/generar-csv-donantes.sh`.

### Servicios y puertos

| Proceso | Puerto | Swagger / salud |
|---|---|---|
| Front (SSR, BFF) | 8080 | — |
| Donaciones | 8081 | <http://localhost:8081/swagger-ui.html> |
| Logística | 8082 | <http://localhost:8082/swagger-ui.html> |
| Incentivos | 8083 | <http://localhost:8083/swagger-ui.html> |
| Notificaciones | 8084 | <http://localhost:8084/swagger-ui.html> |
| Autenticación | 8085 | <http://localhost:8085/swagger-ui.html> · JWKS en `/.well-known/jwks.json` |
| Broker Artemis | 61616 (8161 salud) | — |
| Planificador de rutas (simulación del proveedor externo) | 8090 | — |

Para usar Swagger con endpoints protegidos: `POST /api/auth/login` en Autenticación y pegar el `accessToken` en *Authorize*.
Requerimiento de la Entrega 1: `GET http://localhost:8081/api/saludo` → `Hola desde el servicio de Donaciones.`

---

## Estructura del repositorio

```
├── plataforma/
│   ├── contratos/            Shared kernel: eventos, mensajes y excepciones base (Java puro)
│   └── soporte/              Auto-configuración transversal: JMS, JWT, Problem Details, OpenAPI
├── servicios/
│   ├── autenticacion/        Usuarios, BCrypt, JWT RS256 + JWKS, activación de cuentas
│   ├── donaciones/           Donantes, entidades, necesidades, donaciones, matchmaking, importación CSV
│   ├── logistica/            Flota, entregas, rutas, seguimiento GPS (MongoDB)
│   ├── incentivos/           Misiones, insignias, métricas, ranking, difusión n8n
│   └── notificaciones/       Email / SMS / WhatsApp con reintentos
├── front/                    Cliente liviano Spring MVC + Thymeleaf (BFF)
├── infraestructura-local/broker/   Artemis embebido (reemplazable por un broker gestionado)
├── externos/planificador-rutas/    Simulación del componente externo de planificación
├── docs/                     Arquitectura, modelo, justificaciones, diagramas, n8n, datos de prueba
└── scripts/                  Arranque/parada y generador de CSV
```

Cada servicio sigue **Arquitectura Hexagonal** con estructura que "grita" el negocio:
`<contexto>/domain` (Java puro) → `<contexto>/application` (casos de uso) → `<contexto>/infrastructure` (adaptadores).

## Tests

```bash
./mvnw test        # 110 tests: dominio, casos de uso con fakes, e integración HTTP + JPA + seguridad
```

## Configuración para integraciones reales

| Variable | Efecto |
|---|---|
| `EMAIL_MODO=smtp` + `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `EMAIL_REMITENTE` | Envío real de correos (Notificaciones) |
| `TWILIO_MODO=real` + `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_NUMERO_SMS`, `TWILIO_NUMERO_WHATSAPP` | SMS y WhatsApp reales |
| `N8N_WEBHOOK_URL` | Activa la difusión de insignias en redes (Incentivos) |
| `SPRING_PROFILES_ACTIVE=postgres` + `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL en lugar de H2 (servicios relacionales) |
| `SPRING_PROFILES_ACTIVE=mongo` + `MONGO_URI` | MongoDB externo en lugar del embebido (Logística) |
| `SECRETO_DONACIONES`, `SECRETO_LOGISTICA`, `SECRETO_INCENTIVOS`, `TOKEN_CALLBACK` | Secretos de servicio (cambiar fuera de desarrollo) |

## Trazabilidad de requerimientos de UI/UX

| # | Requerimiento | Dónde |
|---|---|---|
| 1 | Landing pública con propósito, destacadas del último mes, accesos | `/` |
| 2 | Mapa de donaciones entregadas con detalle por marcador | `/mapa` |
| 3 | Registro de donantes y entidades | `/registro` |
| 4 | Inicio de sesión de todos los roles | `/login` |
| 5 | Donante filtra por estado y categoría/subcategoría | `/donante/donaciones` |
| 6 | Donante navega entidades beneficiarias | `/donante/entidades` |
| 7 | Persona humana: misiones e insignias | `/donante/incentivos` |
| 8 | Notificaciones al donante (asignación, misión, categoría, recepción) | `/notificaciones` + medio preferido |
| 9 | Donante sigue camiones en el mapa | `/donante/seguimiento` |
| 10 | Entidad registra necesidades | `/entidad/necesidades` |
| 11 | Entidad ve el estado de sus donaciones | `/entidad/donaciones` |
| 12 | Entidad confirma recepción con fotos | `/entidad/donaciones` |
| 13 | Notificaciones a la entidad | `/notificaciones` |
| 14 | Entidad sigue camiones en el mapa | `/entidad/seguimiento` |
| 15 | Admin registra donantes y donaciones | `/admin/donantes/nuevo`, `/admin/donaciones/nueva` |
| 16 | Admin marca donaciones vencidas | `/admin/donaciones/{id}` |
| 17 | Admin elige la entidad final según los algoritmos | `/admin/asignaciones/{id}` |
| 18 | Admin administra camiones | `/admin/camiones` |
| 19 | Ranking mensual e historial | `/admin/rankings` |
| 20 | Importación CSV de +10.000 filas | `/admin/importaciones` |
