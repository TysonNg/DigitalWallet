FROM golang:1.24.8-bookworm AS build
ARG MINIO_COMMIT=9e49d5e7a648f00e26f2246f4dc28e6b07f8c84a
ARG MC_COMMIT=7394ce0dd2a80935aded936b09fa12cbb3cb8096
RUN git clone https://github.com/minio/minio.git /src/minio && cd /src/minio && git checkout ${MINIO_COMMIT} && CGO_ENABLED=0 go build -trimpath -o /out/minio .
RUN git clone https://github.com/minio/mc.git /src/mc && cd /src/mc && git checkout ${MC_COMMIT} && CGO_ENABLED=0 go build -trimpath -o /out/mc .
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl && rm -rf /var/lib/apt/lists/* && useradd --uid 10001 --create-home media && mkdir /data && chown media:media /data
COPY --from=build /out/minio /out/mc /usr/local/bin/
USER media
EXPOSE 9000 9001
ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]