CREATE TABLE media_files (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    bucket VARCHAR(255) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(32) NOT NULL CHECK (content_type IN ('image/png','image/jpeg')),
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','READY','DELETING','DELETED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(bucket,object_key)
);
CREATE INDEX idx_media_owner ON media_files(owner_id);
CREATE INDEX idx_media_cleanup ON media_files(status,updated_at) WHERE status IN ('PENDING','DELETING');