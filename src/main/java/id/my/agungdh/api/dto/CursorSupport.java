package id.my.agungdh.api.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class CursorSupport {

    private CursorSupport() {}

    public static void validateSize(int size) {
        if (size < 1 || size > CursorResponse.MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size must be between 1 and " + CursorResponse.MAX_SIZE);
        }
    }

    public static UUID parseOrNull(String cursor) {
        if (cursor == null) return null;
        try {
            return UUID.fromString(cursor);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    public static <E> Long resolveId(
            Function<UUID, Optional<E>> finder,
            UUID uuid,
            Function<E, Long> idExtractor) {
        return finder.apply(uuid)
                .map(idExtractor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"));
    }

    public static <E, D> CursorResponse<D> build(
            List<E> entities,
            int size,
            Function<E, D> mapper,
            Function<E, UUID> uuidExtractor) {
        boolean hasMore = entities.size() > size;
        List<E> page = hasMore ? entities.subList(0, size) : entities;
        List<D> data = page.stream().map(mapper).toList();
        String nextCursor = hasMore ? uuidExtractor.apply(page.get(page.size() - 1)).toString() : null;
        return new CursorResponse<>(data, new CursorResponse.Meta(nextCursor, size, hasMore));
    }
}
