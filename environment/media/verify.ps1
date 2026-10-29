$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$composeFile = Join-Path $PSScriptRoot 'compose.test.yml'
$project = 'dw-media-it-' + [guid]::NewGuid().ToString('N').Substring(0, 10)
$names = @('MEDIA_TEST_DB_USER','MEDIA_TEST_DB_PASSWORD','MEDIA_TEST_ROOT_PASSWORD','MEDIA_S3_ACCESS_KEY','MEDIA_S3_SECRET_KEY')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $env:MEDIA_TEST_DB_USER = 'media_test'
    $env:MEDIA_TEST_DB_PASSWORD = [guid]::NewGuid().ToString('N')
    $env:MEDIA_TEST_ROOT_PASSWORD = [guid]::NewGuid().ToString('N')
    $env:MEDIA_S3_ACCESS_KEY = 'media-test-app'
    $env:MEDIA_S3_SECRET_KEY = [guid]::NewGuid().ToString('N')
    docker compose -p $project -f $composeFile up -d --wait postgres minio
    if ($LASTEXITCODE -ne 0) { throw 'Test service startup failed' }
    docker compose -p $project -f $composeFile run --rm minio-init
    if ($LASTEXITCODE -ne 0) { throw 'Private bucket initialization failed' }
    # Run twice to verify initialization is idempotent.
    docker compose -p $project -f $composeFile run --rm minio-init
    if ($LASTEXITCODE -ne 0) { throw 'Repeated initialization failed' }
    $container = (docker compose -p $project -f $composeFile ps -q minio).Trim()
    & mvn -f (Join-Path $repoRoot 'backend/pom.xml') -pl digitalwallet-infrastructure -am clean test '-Dtest=MediaStorageIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dmedia.test.jdbc=jdbc:postgresql://localhost:15432/media_test' '-Dmedia.test.endpoint=http://localhost:19000' "-Dmedia.test.restart-container=$container"
    if ($LASTEXITCODE -ne 0) { throw 'Media integration tests failed' }
} finally {
    # Only delete resources belonging to this invocation's generated test project.
    if ($project -match '^dw-media-it-[0-9a-f]{10}$') {
        docker compose -p $project -f $composeFile down --volumes --remove-orphans
    }
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
}