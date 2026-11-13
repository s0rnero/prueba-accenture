CREATE TABLE franchises (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(120) NOT NULL,
    version    INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX franchises_name_lower_uidx ON franchises (lower(name));

CREATE TABLE branches (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    franchise_id UUID         NOT NULL REFERENCES franchises (id) ON DELETE CASCADE,
    name         VARCHAR(120) NOT NULL,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX branches_name_lower_uidx ON branches (franchise_id, lower(name));
CREATE INDEX branches_franchise_idx ON branches (franchise_id);

CREATE TABLE products (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id  UUID         NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    stock      INTEGER      NOT NULL CHECK (stock >= 0),
    version    INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX products_name_lower_uidx ON products (branch_id, lower(name));
CREATE INDEX products_branch_idx ON products (branch_id);

CREATE TABLE user_franchises (
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    franchise_id UUID NOT NULL REFERENCES franchises (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, franchise_id)
);

CREATE INDEX user_franchises_user_idx ON user_franchises (user_id);
