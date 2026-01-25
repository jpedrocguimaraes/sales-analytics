package br.com.fourzerofourdev.salesanalyticsbackend.dto;

public record ServerAnalyticsDTO(
        Integer maxPlayers,
        Integer minPlayers,
        String peakTime,
        Double averagePlayers,
        ChartDataDTO chartData
) {}