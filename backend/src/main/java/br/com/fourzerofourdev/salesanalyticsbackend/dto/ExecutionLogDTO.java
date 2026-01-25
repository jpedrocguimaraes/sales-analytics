package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.LogType;

import java.time.LocalDateTime;

public record ExecutionLogDTO(
        Long id,
        LocalDateTime startTime,
        Long durationMs,
        ExecutionStatus status,
        LogType type,
        Integer newCustomersCount,
        Integer newSalesCount,
        Integer onlinePlayersCount,
        String message
) {}