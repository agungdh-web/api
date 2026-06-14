package id.my.agungdh.api.controller;

import id.my.agungdh.api.dto.CategoryDTO;
import id.my.agungdh.api.dto.PageResponse;
import id.my.agungdh.api.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public PageResponse<CategoryDTO> findAll(Pageable pageable) {
        return categoryService.findAll(pageable);
    }

    @GetMapping("/{uuid}")
    public CategoryDTO findByUuid(@PathVariable UUID uuid) {
        return categoryService.findByUuid(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDTO create(@Valid @RequestBody CategoryDTO dto) {
        return categoryService.create(dto);
    }

    @PutMapping("/{uuid}")
    public CategoryDTO update(@PathVariable UUID uuid, @Valid @RequestBody CategoryDTO dto) {
        return categoryService.update(uuid, dto);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid) {
        categoryService.delete(uuid);
    }
}
