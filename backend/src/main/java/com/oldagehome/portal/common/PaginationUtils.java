package com.oldagehome.portal.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

public final class PaginationUtils {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable buildPageable(int page, int size, String sort, String direction, String defaultSort) {
        int safePage = Math.max(page, 0);
        int safeSize = resolvePageSize(size);
        String resolvedSort = hasText(sort) ? sort : defaultSort;
        Sort.Direction resolvedDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(resolvedDirection, resolvedSort));
    }

    public static int resolvePageSize(int size) {
        if (size < MIN_PAGE_SIZE) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static String buildQueryString(Map<String, ?> params) {
        StringJoiner joiner = new StringJoiner("&");

        for (Map.Entry<String, ?> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            String text = String.valueOf(value);
            if (text.isBlank()) {
                continue;
            }

            joiner.add(encode(entry.getKey()) + "=" + encode(text));
        }

        return joiner.toString();
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}