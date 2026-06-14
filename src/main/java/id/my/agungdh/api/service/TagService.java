package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.dto.CursorSupport;
import id.my.agungdh.api.dto.TagDTO;
import id.my.agungdh.api.entity.Tag;
import id.my.agungdh.api.mapper.TagMapper;
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
public class TagService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name");
    private static final Map<String, Function<Tag, Comparable<?>>> SORT_VALUE_EXTRACTORS = Map.of(
            "id", Tag::getId,
            "name", Tag::getName
    );

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Transactional
    public TagDTO create(TagDTO dto) {
        Tag tag = tagMapper.toEntity(dto);
        return tagMapper.toDTO(tagRepository.save(tag));
    }

    @Transactional(readOnly = true)
    public CursorResponse<TagDTO> findAll(String cursor, String sort, String dir, int size) {
        CursorSupport.validateSize(size);
        UUID cursorUuid = CursorSupport.parseOrNull(cursor);
        CursorSupport.ParsedSort parsed = (sort == null || sort.isBlank())
                ? new CursorSupport.ParsedSort("id", Sort.Direction.DESC, CursorSupport.DEFAULT_SORT)
                : CursorSupport.parseSort(sort, dir, ALLOWED_SORT_FIELDS);
        Pageable pageable = PageRequest.of(0, size + 1, parsed.sort());

        Specification<Tag> spec = null;
        if (cursorUuid != null) {
            Tag cursorEntity = tagRepository.findByUuid(cursorUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"));
            Comparable<?> sortValue = SORT_VALUE_EXTRACTORS.get(parsed.field()).apply(cursorEntity);
            spec = CursorSupport.whereAfterCursor(cursorEntity.getId(), sortValue, parsed.field(), parsed.dir());
        }

        List<Tag> entities = tagRepository.findAll(spec, pageable).getContent();
        return CursorSupport.build(entities, size, tagMapper::toDTO, Tag::getUuid);
    }

    @Transactional(readOnly = true)
    public TagDTO findByUuid(UUID uuid) {
        return tagRepository.findByUuid(uuid)
                .map(tagMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    @Transactional
    public TagDTO update(UUID uuid, TagDTO dto) {
        Tag tag = tagRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
        tagMapper.updateEntity(dto, tag);
        return tagMapper.toDTO(tag);
    }

    @Transactional
    public void delete(UUID uuid) {
        if (!tagRepository.existsByUuid(uuid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }
        tagRepository.deleteByUuid(uuid);
    }
}
