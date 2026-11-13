#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

CERT_DIR="infra/docker/certs"
DOMAIN="prueba-accenture.com"

command -v mkcert >/dev/null 2>&1 || {
  echo "mkcert no esta instalado. Alternativa sin mkcert (autofirmado, el navegador avisara):"
  echo "openssl req -x509 -newkey rsa:2048 -nodes -days 825 \\"
  echo "  -keyout $CERT_DIR/$DOMAIN-key.pem -out $CERT_DIR/$DOMAIN.pem \\"
  echo "  -subj \"/CN=$DOMAIN\" -addext \"subjectAltName=DNS:$DOMAIN\""
  exit 1
}

mkdir -p "$CERT_DIR"

mkcert -install

mkcert \
  -cert-file "$CERT_DIR/$DOMAIN.pem" \
  -key-file "$CERT_DIR/$DOMAIN-key.pem" \
  "$DOMAIN" "*.$DOMAIN" localhost 127.0.0.1 ::1

echo
echo "Certificado generado en $CERT_DIR/"
echo "Falta que el dominio resuelva a 127.0.0.1. Agrega esta linea a:"
echo "  C:\\Windows\\System32\\drivers\\etc\\hosts"
echo "  127.0.0.1 $DOMAIN"
