package com.authenticationservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stable pagination DTO to avoid direct serialization of PageImpl.
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}

