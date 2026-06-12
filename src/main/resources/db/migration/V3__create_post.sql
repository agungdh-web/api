CREATE TABLE post (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content TEXT,
    excerpt VARCHAR(500),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    category_id BIGINT NOT NULL REFERENCES category(id)
);
CREATE INDEX idx_post_uuid ON post USING hash (uuid);
CREATE INDEX idx_post_slug ON post (slug);
CREATE INDEX idx_post_category_id ON post (category_id);
