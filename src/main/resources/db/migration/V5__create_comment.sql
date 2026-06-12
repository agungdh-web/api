CREATE TABLE comment (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL,
    content TEXT NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    post_id BIGINT NOT NULL REFERENCES post(id),
    parent_id BIGINT REFERENCES comment(id)
);
CREATE INDEX idx_comment_uuid ON comment USING hash (uuid);
CREATE INDEX idx_comment_post_id ON comment (post_id);
CREATE INDEX idx_comment_parent_id ON comment (parent_id);
