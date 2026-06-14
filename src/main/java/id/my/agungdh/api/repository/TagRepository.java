package id.my.agungdh.api.repository;

import id.my.agungdh.api.entity.Tag;

import java.util.List;
import java.util.UUID;

public interface TagRepository extends BaseRepository<Tag> {
    boolean existsBySlug(String slug);
    List<Tag> findByUuidIn(List<UUID> uuids);
}
