# =============================================================================
#  DonaTrack - arranque local en Windows (PowerShell)
#  Uso:  .\scripts\iniciar.ps1                  (compila si hace falta, levanta todo y el vigilante)
#        .\scripts\iniciar.ps1 -Recompilar
#        .\scripts\iniciar.ps1 -SinVigilante     (sin reinicio automático)
#  Requisitos: JDK 21 (JAVA_HOME o java en el PATH). Maven NO es necesario (usa mvnw).
#  Componentes: scripts\componentes.csv (única fuente, compartida con iniciar.sh y el vigilante).
# =============================================================================
param([switch]$Recompilar, [switch]$SinVigilante)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "comun.ps1")
Set-Location $Raiz
New-Item -ItemType Directory -Force -Path "$Raiz\logs" | Out-Null

$componentes = Get-Componentes

$faltaAlguno = $componentes | Where-Object { -not (Test-Path $_.jar) }
if ($Recompilar -or $faltaAlguno) {
    Write-Host "Compilando todos los módulos (la primera vez descarga dependencias)..." -ForegroundColor Cyan
    # clean: sin él, maven-jar-plugin NO rearma el jar de un módulo cuyo código no cambió aunque
    # haya cambiado una dependencia interna (plataforma/*), y se ejecutaría la versión vieja.
    & .\mvnw.cmd -q -B -DskipTests clean package
    if ($LASTEXITCODE -ne 0) { throw "Falló la compilación" }
}

# Trazas distribuidas: sólo se exportan si el stack de observabilidad (Tempo) está levantado.
if (Test-Salud "http://localhost:3200/ready" 2) {
    $env:DONATRACK_TRAZAS = "true"
    Write-Host "Stack de observabilidad detectado: trazas activadas (Grafana en http://localhost:3000)" -ForegroundColor Cyan
}

$caidos = @()
foreach ($c in $componentes) {
    Write-Host ("Iniciando {0,-15}" -f $c.nombre) -NoNewline
    Iniciar-Componente $c | Out-Null
    # broker y autenticación primero: el resto depende de ellos al arrancar.
    if ($c.nombre -in @("broker", "autenticacion")) {
        if (Esperar-Salud $c) { Write-Host " OK" -ForegroundColor Green }
        else { Write-Host " SIN RESPUESTA" -ForegroundColor Red; $caidos += $c.nombre }
    } else { Write-Host " lanzado" }
}

foreach ($c in $componentes | Where-Object { $_.nombre -notin @("broker", "autenticacion") }) {
    if (Esperar-Salud $c) { Write-Host ("{0,-15} OK" -f $c.nombre) -ForegroundColor Green }
    else { Write-Host ("{0,-15} SIN RESPUESTA" -f $c.nombre) -ForegroundColor Red; $caidos += $c.nombre }
}

if (-not $SinVigilante) {
    $v = Start-Process -FilePath "powershell" -WindowStyle Hidden -PassThru `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "vigilante.ps1"))
    $v.Id | Set-Content (Join-Path $DirPids "vigilante.pid")
    Write-Host "Vigilante activo (reinicio automático). Eventos en logs\vigilante.json" -ForegroundColor Cyan
}

Write-Host ""
if ($caidos.Count -gt 0) {
    Write-Host ("DonaTrack arrancó INCOMPLETO. Sin respuesta: {0}" -f ($caidos -join ", ")) -ForegroundColor Red
    Write-Host "Revisá logs\<componente>.log y logs\<componente>.err.log. Para detener: .\scripts\detener.ps1"
    exit 1
}
Write-Host "DonaTrack está corriendo en http://localhost:8080" -ForegroundColor Green
Write-Host "Cuentas demo (clave Demo1234!): admin@donatrack.org, ana@donatrack.org, escuela10@donatrack.org, chofer@donatrack.org"
Write-Host "Para detener: .\scripts\detener.ps1"
