package com.ckrey.autobackworkflow.common.api;

import com.ckrey.autobackworkflow.common.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/** A stable, opaque cursor page ordered by createdAt DESC, id DESC. */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasMore) {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    public static int limit(Integer requested) {
        int value = requested == null ? DEFAULT_LIMIT : requested;
        if (value < 1 || value > MAX_LIMIT) {
            throw BizException.badRequest("COMMON_INVALID_PAGE", "limit 必须在 1 到 100 之间");
        }
        return value;
    }

    public static Cursor decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            return new Cursor(new Date(Long.parseLong(parts[0])), Long.parseLong(parts[1]));
        } catch (IllegalArgumentException ex) {
            throw BizException.badRequest("COMMON_INVALID_CURSOR", "cursor 格式不合法");
        }
    }

    public static <T> CursorPage<T> from(List<T> fetched, int limit,
                                         Function<T, Date> createdAt, Function<T, Long> id) {
        boolean hasMore = fetched.size() > limit;
        List<T> items = hasMore ? List.copyOf(fetched.subList(0, limit)) : List.copyOf(fetched);
        String next = hasMore ? encode(createdAt.apply(items.getLast()), id.apply(items.getLast())) : null;
        return new CursorPage<>(items, next, hasMore);
    }

    private static String encode(Date createdAt, Long id) {
        String raw = createdAt.getTime() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public record Cursor(Date createdAt, Long id) { }
}
