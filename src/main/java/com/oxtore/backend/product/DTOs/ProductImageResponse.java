package com.oxtore.backend.product.DTOs;

public record ProductImageResponse(
        Long id,
        String url,
        Integer displayOrder
) {
}
