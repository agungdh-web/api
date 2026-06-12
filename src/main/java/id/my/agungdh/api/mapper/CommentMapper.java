package id.my.agungdh.api.mapper;

import id.my.agungdh.api.dto.CommentDTO;
import id.my.agungdh.api.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "postUuid", source = "post.uuid")
    @Mapping(target = "parentUuid", source = "parent.uuid")
    CommentDTO toDTO(Comment entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "replies", ignore = true)
    Comment toEntity(CommentDTO dto);

    List<CommentDTO> toDTOList(List<Comment> entities);
}
