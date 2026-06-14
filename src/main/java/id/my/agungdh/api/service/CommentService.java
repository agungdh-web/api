package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CommentDTO;
import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.dto.CursorSupport;
import id.my.agungdh.api.entity.Comment;
import id.my.agungdh.api.entity.Post;
import id.my.agungdh.api.mapper.CommentMapper;
import id.my.agungdh.api.repository.CommentRepository;
import id.my.agungdh.api.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "createdAt");
    private static final Map<String, Function<Comment, Comparable<?>>> SORT_VALUE_EXTRACTORS = Map.of(
            "id", Comment::getId,
            "createdAt", Comment::getCreatedAt
    );

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentDTO create(CommentDTO dto) {
        Comment comment = commentMapper.toEntity(dto);
        comment.setUuid(UUID.randomUUID());
        resolveRelations(comment, dto);
        comment = commentRepository.save(comment);
        return commentMapper.toDTO(comment);
    }

    @Transactional(readOnly = true)
    public CursorResponse<CommentDTO> findAll(String cursor, String sort, String dir, int size) {
        CursorSupport.validateSize(size);
        UUID cursorUuid = CursorSupport.parseOrNull(cursor);
        CursorSupport.ParsedSort parsed = (sort == null || sort.isBlank())
                ? new CursorSupport.ParsedSort("id", Sort.Direction.DESC, CursorSupport.DEFAULT_SORT)
                : CursorSupport.parseSort(sort, dir, ALLOWED_SORT_FIELDS);
        Pageable pageable = PageRequest.of(0, size + 1, parsed.sort());

        Specification<Comment> spec = (root, q, cb) -> cb.isNull(root.get("parent"));
        if (cursorUuid != null) {
            Comment cursorEntity = commentRepository.findByUuid(cursorUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"));
            Comparable<?> sortValue = SORT_VALUE_EXTRACTORS.get(parsed.field()).apply(cursorEntity);
            spec = spec.and(CursorSupport.whereAfterCursor(cursorEntity.getId(), sortValue, parsed.field(), parsed.dir()));
        }

        List<Comment> entities = commentRepository.findAll(spec, pageable).getContent();
        return CursorSupport.build(entities, size, commentMapper::toDTO, Comment::getUuid);
    }

    @Transactional(readOnly = true)
    public CommentDTO findByUuid(UUID uuid) {
        return commentRepository.findByUuid(uuid)
                .map(commentMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    @Transactional
    public CommentDTO update(UUID uuid, CommentDTO dto) {
        Comment comment = commentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        commentMapper.updateEntity(dto, comment);
        resolveRelations(comment, dto);
        comment = commentRepository.save(comment);
        return commentMapper.toDTO(comment);
    }

    @Transactional
    public void delete(UUID uuid) {
        Comment comment = commentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (comment.getReplies() != null) {
            for (Comment reply : comment.getReplies()) {
                reply.setParent(null);
                commentRepository.save(reply);
            }
        }
        commentRepository.delete(comment);
    }

    private void resolveRelations(Comment comment, CommentDTO dto) {
        Post post = postRepository.findByUuid(dto.postUuid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        comment.setPost(post);

        if (dto.parentUuid() != null) {
            Comment parent = commentRepository.findByUuid(dto.parentUuid())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));
            comment.setParent(parent);
        }
    }
}
