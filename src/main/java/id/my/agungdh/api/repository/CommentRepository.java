package id.my.agungdh.api.repository;

import id.my.agungdh.api.entity.Comment;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentRepository extends BaseRepository<Comment> {
    List<Comment> findByParentIsNullOrderByIdDesc(Pageable pageable);
    List<Comment> findByParentIsNullAndIdLessThanOrderByIdDesc(Long id, Pageable pageable);
}
