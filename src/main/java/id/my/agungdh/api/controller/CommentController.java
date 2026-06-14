package id.my.agungdh.api.controller;

import id.my.agungdh.api.dto.CommentDTO;
import id.my.agungdh.api.dto.PageResponse;
import id.my.agungdh.api.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public PageResponse<CommentDTO> findAll(Pageable pageable) {
        return commentService.findAll(pageable);
    }

    @GetMapping("/{uuid}")
    public CommentDTO findByUuid(@PathVariable UUID uuid) {
        return commentService.findByUuid(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDTO create(@Valid @RequestBody CommentDTO dto) {
        return commentService.create(dto);
    }

    @PutMapping("/{uuid}")
    public CommentDTO update(@PathVariable UUID uuid, @Valid @RequestBody CommentDTO dto) {
        return commentService.update(uuid, dto);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid) {
        commentService.delete(uuid);
    }
}
