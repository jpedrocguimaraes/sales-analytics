package br.com.fourzerofourdev.salesanalyticsbackend.dto;

public record ExternalServerStatusDTO(
        int onlinePlayers,
        int maxPlayers
) {}