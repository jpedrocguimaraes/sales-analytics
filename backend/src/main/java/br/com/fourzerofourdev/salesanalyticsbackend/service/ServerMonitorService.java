package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ExternalServerStatusDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ExecutionLog;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ServerStatusSnapshot;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.LogType;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ExecutionLogRepository;
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
import java.time.temporal.ChronoUnit;

@Service
public class ServerMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMonitorService.class);

    private final ServerStatusRepository serverStatusRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final RestClient restClient;

    @Value("${app.server-status.target-url}")
    private String targetUrl;

    public ServerMonitorService(ServerStatusRepository serverStatusRepository, ExecutionLogRepository executionLogRepository) {
        this.serverStatusRepository = serverStatusRepository;
        this.executionLogRepository = executionLogRepository;
        this.restClient = RestClient.create();
    }

    @Scheduled(cron = "0 0/5 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    public void fetchServerStatus() {
        LocalDateTime start = LocalDateTime.now();
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        String message = null;
        Integer onlinePlayers = null;

        try {
            ExternalServerStatusDTO externalStatus = restClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .body(ExternalServerStatusDTO.class);

            if(externalStatus != null) {
                onlinePlayers = externalStatus.onlinePlayers();

                serverStatusRepository.save(ServerStatusSnapshot.builder()
                        .onlinePlayers(onlinePlayers)
                        .timestamp(LocalDateTime.now())
                        .build());

                LOGGER.debug("Server status saved: {} players online", onlinePlayers);
            } else {
                status = ExecutionStatus.FAILURE;
                message = "API returned null body";
            }
        } catch(Exception exception) {
            LOGGER.error("Failed to fetch server status", exception);
            status = ExecutionStatus.FAILURE;
            message = exception.getMessage();
        } finally {
            saveExecutionLog(start, status, message, onlinePlayers);
        }
    }

    private void saveExecutionLog(LocalDateTime start, ExecutionStatus status, String message, Integer onlinePlayers) {
        LocalDateTime end = LocalDateTime.now();

        if(message != null && message.length() > 900) {
            message = message.substring(0, 900) + "...";
        }

        ExecutionLog log = ExecutionLog.builder()
                .startTime(start)
                .endTime(end)
                .durationMs(ChronoUnit.MILLIS.between(start, end))
                .status(status)
                .type(LogType.SERVER_STATUS_CRAWLER)
                .onlinePlayersCount(onlinePlayers)
                .message(message)
                .build();

        executionLogRepository.save(log);
    }
}