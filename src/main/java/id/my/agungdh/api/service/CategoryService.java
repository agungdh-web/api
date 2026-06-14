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
public class CategoryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name");
    private static final Map<String, Function<Category, Comparable<?>>> SORT_VALUE_EXTRACTORS = Map.of(
            "id", Category::getId,
            "name", Category::getName
    );

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
    public CursorResponse<CategoryDTO> findAll(String cursor, String sort, String dir, int size) {
        CursorSupport.validateSize(size);
        UUID cursorUuid = CursorSupport.parseOrNull(cursor);
        CursorSupport.ParsedSort parsed = (sort == null || sort.isBlank())
                ? new CursorSupport.ParsedSort("id", Sort.Direction.DESC, CursorSupport.DEFAULT_SORT)
                : CursorSupport.parseSort(sort, dir, ALLOWED_SORT_FIELDS);
        Pageable pageable = PageRequest.of(0, size + 1, parsed.sort());

        Specification<Category> spec = null;
        if (cursorUuid != null) {
            Category cursorEntity = categoryRepository.findByUuid(cursorUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"));
            Comparable<?> sortValue = SORT_VALUE_EXTRACTORS.get(parsed.field()).apply(cursorEntity);
            spec = CursorSupport.whereAfterCursor(cursorEntity.getId(), sortValue, parsed.field(), parsed.dir());
        }

        List<Category> entities = categoryRepository.findAll(spec, pageable).getContent();
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
