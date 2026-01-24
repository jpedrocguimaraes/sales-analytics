package br.com.fourzerofourdev.salesanalyticsbackend.dto;

public record ExternalCustomerDTO(
        String id,
        String username,
        Double total
) {}