package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.util.List;

public record CategoryDTO(
        Long id,
        String name,
        Double totalCategoryRevenue,
        boolean active,
        List<ProductDTO> products
) {}