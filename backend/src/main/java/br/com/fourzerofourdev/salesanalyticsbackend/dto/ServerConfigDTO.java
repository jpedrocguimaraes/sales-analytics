package br.com.fourzerofourdev.salesanalyticsbackend.dto;

import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.CurrencyType;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ServerType;

public record ServerConfigDTO(
        Long id,
        String name,
        ServerType type,
        CurrencyType currency,
        String salesUrl,
        String serverAddress,
        boolean active
) {}