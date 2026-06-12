package id.my.agungdh.api.mapper;

import id.my.agungdh.api.dto.CategoryDTO;
import id.my.agungdh.api.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryDTO dto);

    CategoryDTO toDTO(Category entity);

    List<CategoryDTO> toDTOList(List<Category> entities);
}
