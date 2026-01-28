package br.com.fourzerofourdev.salesanalyticsbackend.dto;

public record ProductDTO(
        Long id,
        String name,
        Double currentPrice,
        Long externalId,
        Long categoryId,
        Long totalSalesCount,
        Double totalRevenue,
        String directLink,
        boolean active
) {}