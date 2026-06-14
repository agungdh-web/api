package id.my.agungdh.api.repository;

import id.my.agungdh.api.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);

    List<Comment> findByParentIsNullOrderByIdDesc(Pageable pageable);
    List<Comment> findByParentIsNullAndIdLessThanOrderByIdDesc(Long id, Pageable pageable);
}
