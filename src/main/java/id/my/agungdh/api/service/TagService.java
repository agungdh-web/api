package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.dto.TagDTO;
import id.my.agungdh.api.entity.Tag;
import id.my.agungdh.api.mapper.TagMapper;
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
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Transactional
    public TagDTO create(TagDTO dto) {
        Tag tag = tagMapper.toEntity(dto);
        tag.setUuid(UUID.randomUUID());
        tag = tagRepository.save(tag);
        return tagMapper.toDTO(tag);
    }

    @Transactional(readOnly = true)
    public CursorResponse<TagDTO> findAll(String cursor, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Tag> entities = (cursor == null)
                ? tagRepository.findAllByOrderByIdDesc(pageable)
                : tagRepository.findByIdLessThanOrderByIdDesc(resolveId(cursor), pageable);

        boolean hasMore = entities.size() > size;
        List<Tag> page = hasMore ? entities.subList(0, size) : entities;
        List<TagDTO> data = page.stream().map(tagMapper::toDTO).toList();
        String nextCursor = hasMore ? page.get(page.size() - 1).getUuid().toString() : null;
        return new CursorResponse<>(data, new CursorResponse.Meta(nextCursor, size, hasMore));
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
        tag = tagRepository.save(tag);
        return tagMapper.toDTO(tag);
    }

    @Transactional
    public void delete(UUID uuid) {
        if (!tagRepository.existsByUuid(uuid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }
        tagRepository.deleteByUuid(uuid);
    }

    private Long resolveId(String cursor) {
        UUID uuid;
        try {
            uuid = UUID.fromString(cursor);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
        return tagRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"))
                .getId();
    }
}
