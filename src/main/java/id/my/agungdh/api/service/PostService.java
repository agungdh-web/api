package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.dto.CursorSupport;
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
public class PostService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id");
    private static final Map<String, Function<Post, Comparable<?>>> SORT_VALUE_EXTRACTORS = Map.of(
            "id", Post::getId
    );

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
    public CursorResponse<PostDTO> findAll(String cursor, String sort, String dir, int size) {
        CursorSupport.validateSize(size);
        UUID cursorUuid = CursorSupport.parseOrNull(cursor);
        CursorSupport.ParsedSort parsed = (sort == null || sort.isBlank())
                ? new CursorSupport.ParsedSort("id", Sort.Direction.DESC, CursorSupport.DEFAULT_SORT)
                : CursorSupport.parseSort(sort, dir, ALLOWED_SORT_FIELDS);
        Pageable pageable = PageRequest.of(0, size + 1, parsed.sort());

        Specification<Post> spec = null;
        if (cursorUuid != null) {
            Post cursorEntity = postRepository.findByUuid(cursorUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"));
            Comparable<?> sortValue = SORT_VALUE_EXTRACTORS.get(parsed.field()).apply(cursorEntity);
            spec = CursorSupport.whereAfterCursor(cursorEntity.getId(), sortValue, parsed.field(), parsed.dir());
        }

        List<Post> entities = postRepository.findAll(spec, pageable).getContent();
        return CursorSupport.build(entities, size, postMapper::toDTO, Post::getUuid);
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
}
