CREATE TABLE tag (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE
);
CREATE INDEX idx_tag_uuid ON tag USING hash (uuid);
