$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sslDir = Join-Path $scriptDir "ssl"

if (-not (Test-Path $sslDir)) {
    New-Item -ItemType Directory -Path $sslDir | Out-Null
}

$opensslPath = "openssl"
if (-not (Get-Command "openssl" -ErrorAction SilentlyContinue)) {
    if (Test-Path "C:\Program Files\Git\usr\bin\openssl.exe") {
        $opensslPath = "C:\Program Files\Git\usr\bin\openssl.exe"
    } else {
        Write-Error "OpenSSL is not found on PATH or Git installation. Please install OpenSSL or Git for Windows."
    }
}

$certFile = Join-Path $sslDir "localhost.crt"
$keyFile = Join-Path $sslDir "localhost.key"

Write-Host "Generating self-signed SSL certificate for localhost..."
$env:MSYS_NO_PATHCONV = "1"
& $opensslPath req -x509 -nodes -days 365 -newkey rsa:2048 `
    -keyout $keyFile `
    -out $certFile `
    -subj "/CN=localhost/O=DigitalWallet/C=VN" `
    -addext "subjectAltName=DNS:localhost,DNS:*.localhost,IP:127.0.0.1"
Remove-Item env:MSYS_NO_PATHCONV -ErrorAction SilentlyContinue

Write-Host "SSL Certificate generated successfully at $sslDir" -ForegroundColor Green
Write-Host " - Certificate: $certFile"
Write-Host " - Private Key: $keyFile"
