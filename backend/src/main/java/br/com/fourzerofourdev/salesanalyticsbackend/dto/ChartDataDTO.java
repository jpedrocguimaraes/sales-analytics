package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.util.List;

public record ChartDataDTO(
        List<String> labels,
        List<ChartSeriesDTO> series
) {}