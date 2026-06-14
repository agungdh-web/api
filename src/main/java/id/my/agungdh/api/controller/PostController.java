package id.my.agungdh.api.controller;

import id.my.agungdh.api.dto.PageResponse;
import id.my.agungdh.api.dto.PostDTO;
import id.my.agungdh.api.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public PageResponse<PostDTO> findAll(Pageable pageable) {
        return postService.findAll(pageable);
    }

    @GetMapping("/{uuid}")
    public PostDTO findByUuid(@PathVariable UUID uuid) {
        return postService.findByUuid(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDTO create(@Valid @RequestBody PostDTO dto) {
        return postService.create(dto);
    }

    @PutMapping("/{uuid}")
    public PostDTO update(@PathVariable UUID uuid, @Valid @RequestBody PostDTO dto) {
        return postService.update(uuid, dto);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid) {
        postService.delete(uuid);
    }
}
