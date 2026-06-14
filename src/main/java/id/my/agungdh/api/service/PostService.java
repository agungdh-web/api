package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.dto.PostDTO;
import id.my.agungdh.api.dto.TagDTO;
import id.my.agungdh.api.entity.Category;
import id.my.agungdh.api.entity.Post;
import id.my.agungdh.api.entity.Tag;
import id.my.agungdh.api.mapper.PostMapper;
import id.my.agungdh.api.repository.CategoryRepository;
import id.my.agungdh.api.repository.PostRepository;
import id.my.agungdh.api.repository.TagRepository;
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
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostMapper postMapper;

    @Transactional
    public PostDTO create(PostDTO dto) {
        Post post = postMapper.toEntity(dto);
        post.setUuid(UUID.randomUUID());
        resolveRelations(post, dto);
        post = postRepository.save(post);
        return postMapper.toDTO(post);
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostDTO> findAll(String cursor, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Post> entities = (cursor == null)
                ? postRepository.findAllByOrderByIdDesc(pageable)
                : postRepository.findByIdLessThanOrderByIdDesc(resolveId(cursor), pageable);

        boolean hasMore = entities.size() > size;
        List<Post> page = hasMore ? entities.subList(0, size) : entities;
        List<PostDTO> data = page.stream().map(postMapper::toDTO).toList();
        String nextCursor = hasMore ? page.get(page.size() - 1).getUuid().toString() : null;
        return new CursorResponse<>(data, new CursorResponse.Meta(nextCursor, size, hasMore));
    }

    @Transactional(readOnly = true)
    public PostDTO findByUuid(UUID uuid) {
        return postRepository.findByUuid(uuid)
                .map(postMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    @Transactional
    public PostDTO update(UUID uuid, PostDTO dto) {
        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        postMapper.updateEntity(dto, post);
        resolveRelations(post, dto);
        post = postRepository.save(post);
        return postMapper.toDTO(post);
    }

    @Transactional
    public void delete(UUID uuid) {
        if (!postRepository.existsByUuid(uuid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        postRepository.deleteByUuid(uuid);
    }

    private void resolveRelations(Post post, PostDTO dto) {
        Category category = categoryRepository.findByUuid(dto.category().uuid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        post.setCategory(category);

        if (dto.tags() != null && !dto.tags().isEmpty()) {
            List<UUID> tagUuids = dto.tags().stream().map(TagDTO::uuid).toList();
            List<Tag> tags = tagRepository.findByUuidIn(tagUuids);
            if (tags.size() != tagUuids.size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some tags not found");
            }
            post.setTags(tags);
        } else {
            post.setTags(List.of());
        }
    }

    private Long resolveId(String cursor) {
        UUID uuid;
        try {
            uuid = UUID.fromString(cursor);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
        return postRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"))
                .getId();
    }
}
