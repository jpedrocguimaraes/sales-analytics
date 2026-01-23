package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.math.BigDecimal;

public record DashboardSummaryDTO(
        BigDecimal totalRevenue,
        BigDecimal averageTicket,
        long totalSalesCount,
        TopDonorDTO topDonor,
        BigDecimal projectedRevenue
) {}