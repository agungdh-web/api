package id.my.agungdh.api.repository;

import id.my.agungdh.api.entity.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);
    boolean existsBySlug(String slug);
    List<Tag> findByUuidIn(List<UUID> uuids);

    List<Tag> findAllByOrderByIdDesc(Pageable pageable);
    List<Tag> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);
}
