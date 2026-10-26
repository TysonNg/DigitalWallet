-- Generic integration outbox. Insert inside the business transaction; publishing is a separate step.
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL CHECK (length(trim(aggregate_type)) > 0),
    aggregate_id VARCHAR(255) NOT NULL CHECK (length(trim(aggregate_id)) > 0),
    event_type VARCHAR(100) NOT NULL CHECK (length(trim(event_type)) > 0),
    schema_version INTEGER NOT NULL CHECK (schema_version > 0),
    destination VARCHAR(100) NOT NULL CHECK (length(trim(destination)) > 0),
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT
);
CREATE INDEX idx_outbox_event_pending ON outbox_event(available_at,created_at,id)
    WHERE published_at IS NULL;
CREATE INDEX idx_outbox_event_aggregate ON outbox_event(aggregate_type,aggregate_id,created_at);
