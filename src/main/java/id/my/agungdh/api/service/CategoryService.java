package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CategoryDTO;
import id.my.agungdh.api.dto.PageResponse;
import id.my.agungdh.api.entity.Category;
import id.my.agungdh.api.mapper.CategoryMapper;
import id.my.agungdh.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public PageResponse<CategoryDTO> findAll(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable).map(categoryMapper::toDTO));
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
}
