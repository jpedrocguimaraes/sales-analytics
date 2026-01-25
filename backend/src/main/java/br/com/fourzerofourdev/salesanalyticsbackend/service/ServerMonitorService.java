package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ExternalServerStatusDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ServerStatusSnapshot;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ServerStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Service
public class ServerMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMonitorService.class);

    private final ServerStatusRepository serverStatusRepository;
    private final RestClient restClient;

    @Value("${app.server-status.target-url}")
    private String targetUrl;

    public ServerMonitorService(ServerStatusRepository serverStatusRepository) {
        this.serverStatusRepository = serverStatusRepository;
        this.restClient = RestClient.create();
    }

    @Scheduled(cron = "0 0/5 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    public void fetchServerStatus() {
        try {
            ExternalServerStatusDTO status = restClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .body(ExternalServerStatusDTO.class);

            if(status != null) {
                serverStatusRepository.save(ServerStatusSnapshot.builder()
                        .onlinePlayers(status.onlinePlayers())
                        .timestamp(LocalDateTime.now())
                        .build());

                LOGGER.debug("Server status saved: {} players online", status.onlinePlayers());
            }
        } catch(Exception exception) {
            LOGGER.error("Failed to fetch server status: {}", exception.getMessage());
        }
    }
}