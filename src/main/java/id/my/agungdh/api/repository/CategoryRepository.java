package id.my.agungdh.api.repository;

import id.my.agungdh.api.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);
    boolean existsBySlug(String slug);

    List<Category> findAllByOrderByIdDesc(Pageable pageable);
    List<Category> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);
}
