CREATE TABLE beneficiaries
(
    id                    UUID                        NOT NULL,
    user_id               UUID                        NOT NULL,
    beneficiary_user_id   UUID                        NOT NULL,
    beneficiary_wallet_id UUID                        NOT NULL,
    nickname              VARCHAR(100),
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_beneficiaries PRIMARY KEY (id),
    CONSTRAINT fk_beneficiaries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_beneficiaries_target_user FOREIGN KEY (beneficiary_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_beneficiaries_target_wallet FOREIGN KEY (beneficiary_wallet_id) REFERENCES wallets (id) ON DELETE CASCADE,
    CONSTRAINT uq_beneficiaries_user_target UNIQUE (user_id, beneficiary_user_id),
    CONSTRAINT chk_beneficiary_not_self CHECK (user_id <> beneficiary_user_id)
);

CREATE INDEX idx_beneficiaries_user_id ON beneficiaries (user_id);
