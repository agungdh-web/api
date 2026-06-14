package id.my.agungdh.api.repository;

import id.my.agungdh.api.entity.Post;

public interface PostRepository extends BaseRepository<Post> {
    boolean existsBySlug(String slug);
}
