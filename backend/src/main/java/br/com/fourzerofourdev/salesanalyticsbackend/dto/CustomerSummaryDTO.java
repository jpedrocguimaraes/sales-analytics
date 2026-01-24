package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.time.LocalDateTime;

public record CustomerSummaryDTO(
        String username,
        Double totalSpent,
        Long purchaseCount,
        LocalDateTime lastPurchaseDate
) {}