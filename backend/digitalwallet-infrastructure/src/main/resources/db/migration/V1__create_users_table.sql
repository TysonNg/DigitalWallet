CREATE TABLE users
(
    id           UUID         NOT NULL,
    full_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NOT NULL,
    phone_number VARCHAR(13)  NOT NULL,
    dob          date         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);