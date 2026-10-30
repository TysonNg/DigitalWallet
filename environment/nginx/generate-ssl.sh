#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SSL_DIR="${SCRIPT_DIR}/ssl"

mkdir -p "${SSL_DIR}"

echo "Generating self-signed SSL certificate for localhost..."
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "${SSL_DIR}/localhost.key" \
    -out "${SSL_DIR}/localhost.crt" \
    -subj "/CN=localhost/O=DigitalWallet/C=VN" \
    -addext "subjectAltName=DNS:localhost,DNS:*.localhost,IP:127.0.0.1"

echo "SSL Certificate generated successfully at ${SSL_DIR}:"
echo " - Certificate: ${SSL_DIR}/localhost.crt"
echo " - Private Key: ${SSL_DIR}/localhost.key"
