package id.my.agungdh.api.mapper;

import id.my.agungdh.api.dto.PostDTO;
import id.my.agungdh.api.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, TagMapper.class})
public interface PostMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "comments", ignore = true)
    Post toEntity(PostDTO dto);

    PostDTO toDTO(Post entity);

    List<PostDTO> toDTOList(List<Post> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "comments", ignore = true)
    void updateEntity(PostDTO dto, @MappingTarget Post entity);
}
