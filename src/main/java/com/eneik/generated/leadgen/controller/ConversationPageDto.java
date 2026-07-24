package com.eneik.generated.leadgen.controller;

import java.util.List;

public class ConversationPageDto {
    private List<ConversationDto> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public ConversationPageDto() {}

    public ConversationPageDto(List<ConversationDto> content, long totalElements, int totalPages, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    public List<ConversationDto> getContent() {
        return content;
    }

    public void setContent(List<ConversationDto> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
