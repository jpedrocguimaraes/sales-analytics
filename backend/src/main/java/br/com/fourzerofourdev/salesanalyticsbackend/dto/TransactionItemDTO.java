package br.com.fourzerofourdev.salesanalyticsbackend.dto;

public record TransactionItemDTO(
        String productName,
        int quantity,
        Long productId
) {}