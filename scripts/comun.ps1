# Funciones compartidas por iniciar.ps1, vigilante.ps1 y detener.ps1 (se cargan con ". .\comun.ps1").

$script:Raiz = Split-Path -Parent $PSScriptRoot
$script:DirPids = Join-Path $script:Raiz "logs\pids"
$script:Java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }

function Get-Componentes {
    Import-Csv (Join-Path $PSScriptRoot "componentes.csv") | ForEach-Object {
        $_ | Add-Member -NotePropertyName salud -NotePropertyValue "http://localhost:$($_.puerto)/actuator/health" -PassThru
    }
}

function Test-Salud([string]$url, [int]$timeout = 3) {
    try { return (Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec $timeout).StatusCode -eq 200 } catch { return $false }
}

function Esperar-Salud($c) {
    $limite = (Get-Date).AddSeconds([int]$c.espera)
    while ((Get-Date) -lt $limite) {
        if (Test-Salud $c.salud 2) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Get-PidComponente([string]$nombre) {
    $archivo = Join-Path $script:DirPids "$nombre.pid"
    if (Test-Path $archivo) { return [int](Get-Content $archivo -TotalCount 1) }
    return $null
}

function Test-ProcesoVivo($id) {
    return $id -and (Get-Process -Id $id -ErrorAction SilentlyContinue)
}

function Iniciar-Componente($c) {
    New-Item -ItemType Directory -Force -Path $script:DirPids | Out-Null
    $log = Join-Path $script:Raiz "logs\$($c.nombre).log"
    # Conservar el log del proceso anterior: si se reinició por una caída, ahí está la causa.
    if (Test-Path $log) { Move-Item -Force $log (Join-Path $script:Raiz "logs\$($c.nombre).anterior.log") }
    $p = Start-Process -FilePath $script:Java -ArgumentList @("-Xmx384m", "-XX:+UseSerialGC", "-jar", $c.jar) `
        -WorkingDirectory $script:Raiz -RedirectStandardOutput $log `
        -RedirectStandardError (Join-Path $script:Raiz "logs\$($c.nombre).err.log") -WindowStyle Hidden -PassThru
    $p.Id | Set-Content (Join-Path $script:DirPids "$($c.nombre).pid")
    return $p.Id
}

function Detener-Componente([string]$nombre) {
    $id = Get-PidComponente $nombre
    if (Test-ProcesoVivo $id) { Stop-Process -Id $id -Force -ErrorAction SilentlyContinue }
    Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $script:DirPids "$nombre.pid")
}

# Log en JSON (mismo formato ECS que los servicios) para que Alloy/Loki lo muestren en Grafana.
function Escribir-LogJson([string]$servicio, [string]$nivel, [string]$mensaje) {
    $linea = [ordered]@{
        "@timestamp" = (Get-Date).ToUniversalTime().ToString("o")
        log          = @{ level = $nivel }
        service      = @{ name = $servicio }
        message      = $mensaje
    } | ConvertTo-Json -Compress
    # UTF-8 SIN BOM: con BOM la primera línea no es JSON válido para Loki.
    [System.IO.File]::AppendAllText((Join-Path $script:Raiz "logs\$servicio.json"), $linea + "`n", (New-Object System.Text.UTF8Encoding $false))
    Write-Host "[$nivel] $mensaje"
}
