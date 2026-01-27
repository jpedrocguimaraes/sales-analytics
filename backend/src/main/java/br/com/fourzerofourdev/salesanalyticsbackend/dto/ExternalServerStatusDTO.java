package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalServerStatusDTO(
        String status,
        boolean online,
        String error,
        @JsonProperty("players") PlayersDTO players
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayersDTO(
            int max,
            int now
    ) {}
}