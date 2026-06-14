package id.my.agungdh.api.dto;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class CursorSupport {

    public static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");

    public record ParsedSort(String field, Sort.Direction dir, Sort sort) {
        public static ParsedSort of(String field, Sort.Direction dir) {
            return new ParsedSort(field, dir, Sort.by(dir, field, "id"));
        }
    }

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

    public static <E> Long resolveId(Function<UUID, Optional<E>> finder, UUID uuid, Function<E, Long> idExtractor) {
        return finder.apply(uuid)
                .map(idExtractor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"));
    }

    public static ParsedSort parseSort(String sort, String dir, Set<String> allowedFields) {
        Sort.Direction direction = Sort.Direction.ASC;
        if (dir != null && !dir.isBlank()) {
            try {
                direction = Sort.Direction.fromString(dir);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dir: " + dir);
            }
        }
        String field = (sort == null || sort.isBlank()) ? "id" : sort;
        if (!allowedFields.contains(field)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field: " + field);
        }
        return ParsedSort.of(field, direction);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <E> Specification<E> whereAfterCursor(
            Long cursorId, Comparable<?> cursorSortValue, String sortField, Sort.Direction dir) {
        if (cursorId == null) return null;
        boolean asc = dir == Sort.Direction.ASC;
        return (root, query, cb) -> {
            Expression sortExpr = root.get(sortField);
            Expression<Long> idExpr = root.get("id");
            Comparable value = (Comparable) cursorSortValue;
            Predicate comp = asc
                    ? cb.greaterThan(sortExpr, value)
                    : cb.lessThan(sortExpr, value);
            Predicate idCmp = asc
                    ? cb.greaterThan(idExpr, cursorId)
                    : cb.lessThan(idExpr, cursorId);
            return cb.or(comp, cb.and(cb.equal(sortExpr, value), idCmp));
        };
    }
}
