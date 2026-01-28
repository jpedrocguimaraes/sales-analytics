package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerTransactionHistoryDTO(
        Long id,
        Double amount,
        LocalDateTime timestamp,
        List<TransactionItemDTO> items
) {}