package com.example.consultingbooking.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {

    private static final int MAX_PAGE_SIZE = 50;

    private PageRequestFactory() {
    }

    public static PageRequest of(int page, int size, Sort sort) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, sort);
    }
}
