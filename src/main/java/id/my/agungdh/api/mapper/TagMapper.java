package id.my.agungdh.api.mapper;

import id.my.agungdh.api.dto.TagDTO;
import id.my.agungdh.api.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {

    @Mapping(target = "id", ignore = true)
    Tag toEntity(TagDTO dto);

    TagDTO toDTO(Tag entity);

    List<TagDTO> toDTOList(List<Tag> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    void updateEntity(TagDTO dto, @MappingTarget Tag entity);
}
