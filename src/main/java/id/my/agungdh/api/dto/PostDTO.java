package id.my.agungdh.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostDTO(
    UUID uuid,
    @NotBlank String title,
    @NotBlank String slug,
    String content,
    String excerpt,
    LocalDateTime publishedAt,
    @NotNull CategoryDTO category,
    List<TagDTO> tags
) {}
