package id.my.agungdh.api.repository;

import id.my.agungdh.api.entity.Category;

public interface CategoryRepository extends BaseRepository<Category> {
    boolean existsBySlug(String slug);
}
