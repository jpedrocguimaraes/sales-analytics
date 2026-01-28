package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GlobalSaleDTO(
        Long id,
        String username,
        LocalDateTime timestamp,
        Double amount,
        List<TransactionItemDTO> items
) {}