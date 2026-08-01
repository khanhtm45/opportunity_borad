package com.opportunityboard.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response chuẩn cho phân trang cursor-based. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private java.util.List<T> items;
    private String nextCursor;
    private long total;
    private boolean hasMore;

    public static <T> PagedResponse<T> of(java.util.List<T> items, String nextCursor, long total) {
        return new PagedResponse<>(items, nextCursor, total, nextCursor != null);
    }
}
