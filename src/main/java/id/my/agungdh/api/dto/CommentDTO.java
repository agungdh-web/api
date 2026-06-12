package id.my.agungdh.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommentDTO(
    UUID uuid,
    @NotBlank String content,
    @NotBlank String authorName,
    String authorEmail,
    LocalDateTime createdAt,
    UUID postUuid,
    UUID parentUuid,
    List<CommentDTO> replies
) {}
