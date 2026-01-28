package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.time.LocalDateTime;

public record ProductPriceHistoryDTO(
        Double price,
        LocalDateTime date
) {}