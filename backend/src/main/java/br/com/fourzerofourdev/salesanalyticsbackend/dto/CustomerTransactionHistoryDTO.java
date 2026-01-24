package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.time.LocalDateTime;

public record CustomerTransactionHistoryDTO(
        Long id,
        Double amount,
        LocalDateTime timestamp
) {}