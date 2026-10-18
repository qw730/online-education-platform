package com.edu.platform.common;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Pagination payload returned to the frontend.
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, int page, int size, int totalPages) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
    }

    /** Builds a page result and maps entities into view objects. */
    public static <E, V> PageResult<V> of(Page<E> page, Function<E, V> mapper) {
        List<V> list = page.getContent().stream().map(mapper).toList();
        return new PageResult<>(list, page.getTotalElements(),
                page.getNumber() + 1, page.getSize(), page.getTotalPages());
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(),
                page.getNumber() + 1, page.getSize(), page.getTotalPages());
    }
}
