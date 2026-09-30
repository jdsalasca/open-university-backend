Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$backendRoot = Join-Path (Split-Path -Parent $PSScriptRoot) 'backend'
$containerName = 'universiry-mysql-contract-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$containerStarted = $false
$databaseName = 'universiry_contract'
$databaseUser = 'contract_runner'
$databasePassword = 'contract-' + [Guid]::NewGuid().ToString('N')
$rootPassword = 'root-' + [Guid]::NewGuid().ToString('N')
$environmentNames = @(
    'UNIVERSIRY_MYSQL_TEST_URL',
    'UNIVERSIRY_MYSQL_TEST_USERNAME',
    'UNIVERSIRY_MYSQL_TEST_PASSWORD'
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI no está disponible en PATH.'
    }
    docker info --format '{{.ServerVersion}}' | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'El motor Docker no está disponible.'
    }

    Write-Host 'Iniciando MySQL 8.4 temporal, aislado y sin volumen persistente...'
    docker run --detach --rm `
        --name $containerName `
        --env "MYSQL_ROOT_PASSWORD=$rootPassword" `
        --env "MYSQL_DATABASE=$databaseName" `
        --env "MYSQL_USER=$databaseUser" `
        --env "MYSQL_PASSWORD=$databasePassword" `
        --publish '127.0.0.1::3306' `
        mysql:8.4 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudo iniciar el contenedor MySQL 8.4.'
    }
    $containerStarted = $true

    $ready = $false
    for ($attempt = 0; $attempt -lt 180; $attempt++) {
        docker exec --env "MYSQL_PWD=$rootPassword" $containerName `
            mysqladmin ping --host=127.0.0.1 --user=root --silent *> $null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        $containerState = docker inspect --format '{{.State.Status}}' $containerName 2>$null
        if ($LASTEXITCODE -ne 0 -or $containerState -ne 'running') {
            docker logs $containerName
            throw "El contenedor MySQL terminó antes de estar listo (estado: $containerState)."
        }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) {
        docker logs $containerName
        throw 'MySQL no quedó listo dentro de 180 segundos.'
    }

    $publishedPort = docker port $containerName '3306/tcp'
    if ($LASTEXITCODE -ne 0 -or $publishedPort -notmatch '^127\.0\.0\.1:(\d+)$') {
        throw "No se pudo resolver el puerto local efímero: $publishedPort"
    }

    $env:UNIVERSIRY_MYSQL_TEST_URL = "jdbc:mysql://127.0.0.1:$($Matches[1])/${databaseName}?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=UTC"
    $env:UNIVERSIRY_MYSQL_TEST_USERNAME = $databaseUser
    $env:UNIVERSIRY_MYSQL_TEST_PASSWORD = $databasePassword

    & (Join-Path $PSScriptRoot 'use-sdkman-java.ps1')
    Push-Location $backendRoot
    try {
        & '.\mvnw.cmd' `
            '-Dtest=AcademicCatalogMySqlContractTest' `
            '-Duniversiry.mysql-contract.enabled=true' `
            '-Duniversiry.mysql-performance.enabled=true' `
            '-Dsurefire.useFile=false' `
            'test'
        $testExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
    if ($testExitCode -ne 0) {
        throw "El contrato MySQL falló con código Maven $testExitCode."
    }
}
finally {
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    if ($containerStarted -and (Get-Command docker -ErrorAction SilentlyContinue)) {
        docker rm --force $containerName *> $null
    }
}
