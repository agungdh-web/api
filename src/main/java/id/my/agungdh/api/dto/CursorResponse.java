package id.my.agungdh.api.dto;

import java.util.List;

public record CursorResponse<T>(List<T> data, Meta meta) {

    public static final int MAX_SIZE = 100;

    public record Meta(String nextCursor, int size, boolean hasMore) {}
}
