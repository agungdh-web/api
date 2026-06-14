package id.my.agungdh.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseRepository<E> extends JpaRepository<E, Long> {
    Optional<E> findByUuid(UUID uuid);
    boolean existsByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);

    List<E> findAllByOrderByIdDesc(Pageable pageable);
    List<E> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);
}
