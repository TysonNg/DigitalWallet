#!/bin/sh
set -eu
# mc configuration lives in the init container only; do not print credentials.
mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null
mc mb --ignore-existing "local/$MEDIA_S3_BUCKET" >/dev/null
mc anonymous set none "local/$MEDIA_S3_BUCKET" >/dev/null
cat > /tmp/media-policy.json <<EOF
{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["s3:GetBucketLocation","s3:ListBucket"],"Resource":["arn:aws:s3:::$MEDIA_S3_BUCKET"]},{"Effect":"Allow","Action":["s3:GetObject","s3:PutObject","s3:DeleteObject"],"Resource":["arn:aws:s3:::$MEDIA_S3_BUCKET/*"]}]}
EOF
mc admin policy create local media-app /tmp/media-policy.json >/dev/null
mc admin user add local "$MEDIA_S3_ACCESS_KEY" "$MEDIA_S3_SECRET_KEY" >/dev/null
mc admin policy attach local media-app --user "$MEDIA_S3_ACCESS_KEY" >/dev/null
