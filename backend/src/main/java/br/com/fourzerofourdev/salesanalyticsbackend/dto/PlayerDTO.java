package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import java.util.UUID;

public record PlayerDTO(
        UUID id,
        String username,
        Double total
) {}