package id.my.agungdh.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> data, Meta meta) {

    public record Meta(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {}

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), toMeta(page));
    }

    private static Meta toMeta(Page<?> page) {
        return new Meta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
