CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE
);
CREATE INDEX idx_category_uuid ON category USING hash (uuid);
