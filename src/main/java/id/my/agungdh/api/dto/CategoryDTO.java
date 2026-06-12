package id.my.agungdh.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryDTO(
    UUID uuid,
    @NotBlank String name,
    @NotBlank String slug
) {}
