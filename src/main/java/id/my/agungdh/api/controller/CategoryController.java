package id.my.agungdh.api.controller;

import id.my.agungdh.api.dto.CategoryDTO;
import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public CursorResponse<CategoryDTO> findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "20") int size) {
        return categoryService.findAll(cursor, sort, dir, size);
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
