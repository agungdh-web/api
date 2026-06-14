package id.my.agungdh.api.controller;

import id.my.agungdh.api.dto.PageResponse;
import id.my.agungdh.api.dto.TagDTO;
import id.my.agungdh.api.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public PageResponse<TagDTO> findAll(Pageable pageable) {
        return tagService.findAll(pageable);
    }

    @GetMapping("/{uuid}")
    public TagDTO findByUuid(@PathVariable UUID uuid) {
        return tagService.findByUuid(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagDTO create(@Valid @RequestBody TagDTO dto) {
        return tagService.create(dto);
    }

    @PutMapping("/{uuid}")
    public TagDTO update(@PathVariable UUID uuid, @Valid @RequestBody TagDTO dto) {
        return tagService.update(uuid, dto);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid) {
        tagService.delete(uuid);
    }
}
