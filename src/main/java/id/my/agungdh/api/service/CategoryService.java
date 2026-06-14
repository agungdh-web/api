package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CategoryDTO;
import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.entity.Category;
import id.my.agungdh.api.mapper.CategoryMapper;
import id.my.agungdh.api.repository.CategoryRepository;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryDTO create(CategoryDTO dto) {
        Category category = categoryMapper.toEntity(dto);
        category.setUuid(UUID.randomUUID());
        category = categoryRepository.save(category);
        return categoryMapper.toDTO(category);
    }

    @Transactional(readOnly = true)
    public CursorResponse<CategoryDTO> findAll(String cursor, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Category> entities = (cursor == null)
                ? categoryRepository.findAllByOrderByIdDesc(pageable)
                : categoryRepository.findByIdLessThanOrderByIdDesc(resolveId(cursor), pageable);

        boolean hasMore = entities.size() > size;
        List<Category> page = hasMore ? entities.subList(0, size) : entities;
        List<CategoryDTO> data = page.stream().map(categoryMapper::toDTO).toList();
        String nextCursor = hasMore ? page.get(page.size() - 1).getUuid().toString() : null;
        return new CursorResponse<>(data, new CursorResponse.Meta(nextCursor, size, hasMore));
    }

    @Transactional(readOnly = true)
    public CategoryDTO findByUuid(UUID uuid) {
        return categoryRepository.findByUuid(uuid)
                .map(categoryMapper::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @Transactional
    public CategoryDTO update(UUID uuid, CategoryDTO dto) {
        Category category = categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        categoryMapper.updateEntity(dto, category);
        category = categoryRepository.save(category);
        return categoryMapper.toDTO(category);
    }

    @Transactional
    public void delete(UUID uuid) {
        if (!categoryRepository.existsByUuid(uuid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        categoryRepository.deleteByUuid(uuid);
    }

    private Long resolveId(String cursor) {
        UUID uuid;
        try {
            uuid = UUID.fromString(cursor);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
        return categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"))
                .getId();
    }
}
