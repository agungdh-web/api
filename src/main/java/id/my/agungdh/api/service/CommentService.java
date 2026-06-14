package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CommentDTO;
import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.entity.Comment;
import id.my.agungdh.api.entity.Post;
import id.my.agungdh.api.mapper.CommentMapper;
import id.my.agungdh.api.repository.CommentRepository;
import id.my.agungdh.api.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

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
    public CursorResponse<CommentDTO> findAll(String cursor, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Comment> entities = (cursor == null)
                ? commentRepository.findByParentIsNullOrderByIdDesc(pageable)
                : commentRepository.findByParentIsNullAndIdLessThanOrderByIdDesc(resolveId(cursor), pageable);

        boolean hasMore = entities.size() > size;
        List<Comment> page = hasMore ? entities.subList(0, size) : entities;
        List<CommentDTO> data = page.stream().map(commentMapper::toDTO).toList();
        String nextCursor = hasMore ? page.get(page.size() - 1).getUuid().toString() : null;
        return new CursorResponse<>(data, new CursorResponse.Meta(nextCursor, size, hasMore));
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

    private Long resolveId(String cursor) {
        UUID uuid;
        try {
            uuid = UUID.fromString(cursor);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
        return commentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"))
                .getId();
    }
}
