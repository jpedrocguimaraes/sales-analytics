package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DashboardSummaryDTO(
        BigDecimal totalRevenue,
        BigDecimal averageTicket,
        long totalSalesCount,
        TopDonorDTO topDonor,
        BigDecimal projectedRevenue,
        LocalDateTime lastUpdate
) {}