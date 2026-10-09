CREATE TABLE notifications
(
    id             UUID         NOT NULL,
    user_id        UUID         NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content        TEXT         NOT NULL,
    transaction_id UUID,
    type           VARCHAR(30)  NOT NULL,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    created_at     TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at ON notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_is_read ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_transaction_id ON notifications (transaction_id);
