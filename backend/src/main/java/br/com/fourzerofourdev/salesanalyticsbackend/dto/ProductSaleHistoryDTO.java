package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.time.LocalDateTime;

public record ProductSaleHistoryDTO(
        String username,
        LocalDateTime timestamp,
        int quantity,
        Double unitPrice,
        Double totalPrice
) {}