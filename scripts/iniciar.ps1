# =============================================================================
#  DonaTrack - arranque local en Windows (PowerShell)
#  Uso:  .\scripts\iniciar.ps1            (compila si hace falta y levanta todo)
#        .\scripts\iniciar.ps1 -Recompilar
#  Requisitos: JDK 21 (JAVA_HOME o java en el PATH). Maven NO es necesario (usa mvnw).
# =============================================================================
param([switch]$Recompilar)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
New-Item -ItemType Directory -Force -Path "$raiz\logs" | Out-Null

$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }

# Orden de arranque: infraestructura -> autenticación -> servicios -> front
$componentes = @(
    @{ nombre = "broker";         jar = "infraestructura-local\broker\target\broker-1.0.0-SNAPSHOT.jar";                    salud = "http://localhost:8161/actuator/health" },
    @{ nombre = "autenticacion";  jar = "servicios\autenticacion\target\servicio-autenticacion-1.0.0-SNAPSHOT.jar";         salud = "http://localhost:8085/actuator/health" },
    @{ nombre = "notificaciones"; jar = "servicios\notificaciones\target\servicio-notificaciones-1.0.0-SNAPSHOT.jar";       salud = "http://localhost:8084/actuator/health" },
    @{ nombre = "donaciones";     jar = "servicios\donaciones\target\servicio-donaciones-1.0.0-SNAPSHOT.jar";               salud = "http://localhost:8081/actuator/health" },
    @{ nombre = "logistica";      jar = "servicios\logistica\target\servicio-logistica-1.0.0-SNAPSHOT.jar";                 salud = "http://localhost:8082/actuator/health" },
    @{ nombre = "incentivos";     jar = "servicios\incentivos\target\servicio-incentivos-1.0.0-SNAPSHOT.jar";               salud = "http://localhost:8083/actuator/health" },
    @{ nombre = "planificador";   jar = "externos\planificador-rutas\target\planificador-rutas-1.0.0-SNAPSHOT.jar";         salud = "http://localhost:8090/actuator/health" },
    @{ nombre = "front";          jar = "front\target\front-1.0.0-SNAPSHOT.jar";                                             salud = "http://localhost:8080/actuator/health" }
)

$faltaAlguno = $componentes | Where-Object { -not (Test-Path $_.jar) }
if ($Recompilar -or $faltaAlguno) {
    Write-Host "Compilando todos los módulos (la primera vez descarga dependencias)..." -ForegroundColor Cyan
    & .\mvnw.cmd -q -B -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Falló la compilación" }
}

function Esperar-Salud($url, $nombre, $segundos) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        try { if ((Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 2).StatusCode -eq 200) { return $true } } catch { }
        Start-Sleep -Seconds 2
    }
    Write-Warning "$nombre no respondió en $segundos s. Revisá logs\$nombre.log"
    return $false
}

$pids = @()
foreach ($c in $componentes) {
    Write-Host ("Iniciando {0,-15}" -f $c.nombre) -NoNewline
    $p = Start-Process -FilePath $java -ArgumentList @("-Xmx384m", "-XX:+UseSerialGC", "-jar", $c.jar) `
        -RedirectStandardOutput "logs\$($c.nombre).log" -RedirectStandardError "logs\$($c.nombre).err.log" `
        -WindowStyle Hidden -PassThru
    $pids += $p.Id
    $espera = if ($c.nombre -eq "logistica") { 240 } else { 120 }   # Mongo embebido se descarga la 1ra vez
    if ($c.nombre -in @("broker", "autenticacion")) {
        if (Esperar-Salud $c.salud $c.nombre $espera) { Write-Host " OK" -ForegroundColor Green }
    } else { Write-Host " lanzado" }
}
$pids | Set-Content "logs\pids.txt"

foreach ($c in $componentes | Where-Object { $_.nombre -notin @("broker", "autenticacion") }) {
    $espera = if ($c.nombre -eq "logistica") { 240 } else { 120 }
    if (Esperar-Salud $c.salud $c.nombre $espera) { Write-Host ("{0,-15} OK" -f $c.nombre) -ForegroundColor Green }
}

Write-Host ""
Write-Host "DonaTrack está corriendo en http://localhost:8080" -ForegroundColor Green
Write-Host "Cuentas demo (clave Demo1234!): admin@donatrack.org, ana@donatrack.org, escuela10@donatrack.org, chofer@donatrack.org"
Write-Host "Para detener: .\scripts\detener.ps1"
