package com.berkan.receiptscanner.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public class PagedResponse<T> {

    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    private PagedResponse() {}

    public static <T> PagedResponse<T> of(Page<T> pageResult) {
        PagedResponse<T> response = new PagedResponse<>();
        response.items = pageResult.getContent();
        response.page = pageResult.getNumber() + 1;
        response.size = pageResult.getSize();
        response.totalElements = pageResult.getTotalElements();
        response.totalPages = pageResult.getTotalPages();
        response.hasNext = pageResult.hasNext();
        response.hasPrevious = pageResult.hasPrevious();
        return response;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasNext() { return hasNext; }
    public boolean isHasPrevious() { return hasPrevious; }
}
