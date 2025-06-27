CREATE TABLE user
(
    user_id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   TEXT        NOT NULL,
    age                    TEXT        NOT NULL,
    address                TEXT        NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
);

