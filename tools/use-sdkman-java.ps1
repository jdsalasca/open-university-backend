Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$sdkmanJavaRoot = Join-Path $env:USERPROFILE '.sdkman\candidates\java'
$sdkmanCurrent = Join-Path $sdkmanJavaRoot 'current'
if (-not (Test-Path -LiteralPath $sdkmanCurrent)) {
    throw "No existe el candidato current de SDKMAN en $sdkmanCurrent. Abre Git Bash, carga SDKMAN y selecciona un JDK."
}

$link = Get-Item -LiteralPath $sdkmanCurrent -Force
$sdkmanTarget = $link.Target
if ($sdkmanTarget -is [array]) {
    $sdkmanTarget = $sdkmanTarget[0]
}
if ([string]::IsNullOrWhiteSpace([string]$sdkmanTarget)) {
    throw "El enlace SDKMAN current no tiene un destino válido: $sdkmanCurrent"
}
if (-not [System.IO.Path]::IsPathRooted([string]$sdkmanTarget)) {
    $sdkmanTarget = Join-Path $sdkmanJavaRoot ([string]$sdkmanTarget)
}

$resolvedJavaHome = (Resolve-Path -LiteralPath $sdkmanTarget).Path
$javaExecutable = Join-Path $resolvedJavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "El candidato SDKMAN no contiene java.exe: $resolvedJavaHome"
}

$javaBin = Join-Path $resolvedJavaHome 'bin'
$remainingPath = $env:PATH -split ';' | Where-Object {
    -not [string]::IsNullOrWhiteSpace($_) -and
    $_ -notmatch '\\.sdkman\\candidates\\java\\[^\\]+\\bin\\?$'
}
$env:JAVA_HOME = $resolvedJavaHome
$env:PATH = (@($javaBin) + $remainingPath) -join ';'

Write-Output "Java de SDKMAN seleccionado para este proceso: $resolvedJavaHome"
