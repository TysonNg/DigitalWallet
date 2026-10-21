-- V13__setup_pgvector.sql
-- Kích hoạt extension vector, hstore và uuid-ossp cho PostgreSQL
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Bảng lưu trữ vector embeddings cho Spring AI (PgVectorStore)
CREATE TABLE IF NOT EXISTS vector_store
(
    id        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding vector(1536)
);

-- Index HNSW (Hierarchical Navigable Small World) tối ưu hóa truy vấn Cosine Similarity
CREATE INDEX IF NOT EXISTS idx_vector_store_hnsw
    ON vector_store USING HNSW (embedding vector_cosine_ops);
