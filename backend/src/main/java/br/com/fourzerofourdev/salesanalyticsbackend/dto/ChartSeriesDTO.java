package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.util.List;

public record ChartSeriesDTO(
        String name,
        List<Number> data,
        String color
) {}