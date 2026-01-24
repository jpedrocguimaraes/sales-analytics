package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;

import java.time.LocalDateTime;

public record ExecutionLogDTO(
        Long id,
        LocalDateTime startTime,
        Long durationMs,
        ExecutionStatus status,
        int newCustomersCount,
        int newSalesCount,
        String message
) {}