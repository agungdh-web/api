package id.my.agungdh.api.repository;

import id.my.agungdh.api.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);
    boolean existsBySlug(String slug);
}
