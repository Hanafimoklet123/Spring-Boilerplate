CREATE TABLE user
(
    name                   TEXT        NOT NULL,
    age                    TEXT        NOT NULL,
    address                TEXT        NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
);

