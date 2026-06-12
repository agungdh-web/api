CREATE TABLE post_tags (
    post_id BIGINT NOT NULL REFERENCES post(id),
    tag_id BIGINT NOT NULL REFERENCES tag(id),
    PRIMARY KEY (post_id, tag_id)
);
CREATE INDEX idx_post_tags_post_id ON post_tags (post_id);
CREATE INDEX idx_post_tags_tag_id ON post_tags (tag_id);
