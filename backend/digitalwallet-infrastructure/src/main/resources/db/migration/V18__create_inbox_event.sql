-- Completed integration-event receipts; provider execution still relies on business idempotency.
CREATE TABLE inbox_event (
    consumer_name VARCHAR(100) NOT NULL CHECK (length(trim(consumer_name)) > 0),
    event_id UUID NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL CHECK (length(trim(aggregate_id)) > 0),
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_name, event_id)
);
CREATE INDEX idx_inbox_event_processed ON inbox_event(processed_at);
