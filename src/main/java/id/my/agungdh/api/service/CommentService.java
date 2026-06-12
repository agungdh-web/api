package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CommentDTO;
import id.my.agungdh.api.entity.Comment;
import id.my.agungdh.api.entity.Post;
import id.my.agungdh.api.mapper.CommentMapper;
import id.my.agungdh.api.repository.CommentRepository;
import id.my.agungdh.api.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public Page<CommentDTO> findAll(Pageable pageable) {
        return commentRepository.findByParentIsNull(pageable).map(commentMapper::toDTO);
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
