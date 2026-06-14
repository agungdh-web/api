package id.my.agungdh.api.service;

import id.my.agungdh.api.dto.CategoryDTO;
import id.my.agungdh.api.dto.CursorResponse;
import id.my.agungdh.api.dto.CursorSupport;
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
        CursorSupport.validateSize(size);
        UUID cursorUuid = CursorSupport.parseOrNull(cursor);
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Category> entities = (cursorUuid == null)
                ? categoryRepository.findAllByOrderByIdDesc(pageable)
                : categoryRepository.findByIdLessThanOrderByIdDesc(
                        CursorSupport.resolveId(categoryRepository::findByUuid, cursorUuid, Category::getId), pageable);
        return CursorSupport.build(entities, size, categoryMapper::toDTO, Category::getUuid);
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
