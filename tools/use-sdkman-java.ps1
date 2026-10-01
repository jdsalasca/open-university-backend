Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$sdkmanJavaRoot = Join-Path $env:USERPROFILE '.sdkman\candidates\java'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sdkmanRcPath = Join-Path $repositoryRoot '.sdkmanrc'
if (-not (Test-Path -LiteralPath $sdkmanRcPath -PathType Leaf)) {
    throw "No existe el archivo de versión SDKMAN del proyecto: $sdkmanRcPath"
}

$javaVersionEntries = @(Get-Content -LiteralPath $sdkmanRcPath | Where-Object { $_ -match '^\s*java\s*=' })
if ($javaVersionEntries.Count -ne 1) {
    throw "El archivo $sdkmanRcPath debe declarar exactamente una versión con java=<version>."
}

if ($javaVersionEntries[0] -notmatch '^\s*java\s*=\s*(?<version>[A-Za-z0-9][A-Za-z0-9._+-]*)\s*(?:#.*)?$') {
    throw "La entrada java de $sdkmanRcPath no contiene una versión SDKMAN válida."
}

$sdkmanJavaVersion = $Matches['version']
$sdkmanJavaVersionHome = Join-Path $sdkmanJavaRoot $sdkmanJavaVersion
if (-not (Test-Path -LiteralPath $sdkmanJavaVersionHome -PathType Container)) {
    throw "No está instalado en SDKMAN el Java $sdkmanJavaVersion fijado por $sdkmanRcPath ($sdkmanJavaVersionHome)."
}

$resolvedJavaHome = (Resolve-Path -LiteralPath $sdkmanJavaVersionHome).Path
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

Write-Output "Java $sdkmanJavaVersion de SDKMAN seleccionado según .sdkmanrc: $resolvedJavaHome"
