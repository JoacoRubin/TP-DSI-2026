# =============================================================================
#  DonaTrack - vigilante: monitoreo proactivo con reinicio automático (Entrega 6)
#  Lo lanza iniciar.ps1 en segundo plano. También se puede correr a mano:
#     .\scripts\vigilante.ps1 [-Intervalo 15] [-FallosParaReiniciar 3]
#
#  Política:
#   - Cada $Intervalo s verifica que el proceso exista y que /actuator/health dé 200.
#   - Reinicia si el proceso murió, o tras $FallosParaReiniciar chequeos fallidos seguidos.
#   - Gracia: después de (re)iniciar no evalúa hasta que pase el tiempo de arranque del componente.
#   - Freno anti-bucle: más de $MaxReinicios reinicios en $VentanaMin minutos => deja de reiniciar
#     ese componente y registra ERROR (un error de configuración no se arregla reiniciando).
#  Todo queda en logs\vigilante.json (visible en Grafana vía Loki).
# =============================================================================
param(
    [int]$Intervalo = 15,
    [int]$FallosParaReiniciar = 3,
    [int]$MaxReinicios = 5,
    [int]$VentanaMin = 10
)
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "comun.ps1")

$componentes = Get-Componentes
$estado = @{}
foreach ($c in $componentes) {
    $estado[$c.nombre] = @{ fallos = 0; reinicios = @(); graciaHasta = (Get-Date).AddSeconds([int]$c.espera); abandonado = $false }
}
Escribir-LogJson "vigilante" "INFO" "Vigilante iniciado: cada $Intervalo s, reinicio tras $FallosParaReiniciar fallos, máx. $MaxReinicios reinicios en $VentanaMin min"

while ($true) {
    foreach ($c in $componentes) {
        $e = $estado[$c.nombre]
        if ($e.abandonado -or (Get-Date) -lt $e.graciaHasta) { continue }

        $vivo = Test-ProcesoVivo (Get-PidComponente $c.nombre)
        $sano = $vivo -and (Test-Salud $c.salud)
        if ($sano) {
            if ($e.fallos -gt 0) { Escribir-LogJson "vigilante" "INFO" "$($c.nombre) se recuperó sin reiniciar" }
            $e.fallos = 0
            continue
        }

        $e.fallos++
        if ($vivo -and $e.fallos -lt $FallosParaReiniciar) {
            Escribir-LogJson "vigilante" "WARN" "$($c.nombre) no responde al health check ($($e.fallos)/$FallosParaReiniciar)"
            continue
        }

        $desde = (Get-Date).AddMinutes(-$VentanaMin)
        $e.reinicios = @($e.reinicios | Where-Object { $_ -gt $desde })
        if ($e.reinicios.Count -ge $MaxReinicios) {
            $e.abandonado = $true
            Escribir-LogJson "vigilante" "ERROR" "$($c.nombre) se reinició $MaxReinicios veces en $VentanaMin min: NO se reintenta más. Revisá logs\$($c.nombre).anterior.log"
            continue
        }

        $motivo = if ($vivo) { "health check fallido $($e.fallos) veces" } else { "el proceso terminó" }
        Escribir-LogJson "vigilante" "WARN" "Reiniciando $($c.nombre) ($motivo)"
        Detener-Componente $c.nombre
        $nuevoPid = Iniciar-Componente $c
        $e.reinicios += Get-Date
        $e.fallos = 0
        $e.graciaHasta = (Get-Date).AddSeconds([int]$c.espera)
        Escribir-LogJson "vigilante" "INFO" "$($c.nombre) relanzado (PID $nuevoPid, reinicio $($e.reinicios.Count) en la ventana)"
    }
    Start-Sleep -Seconds $Intervalo
}
