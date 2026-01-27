package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ExternalServerStatusDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ExecutionLog;
import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ServerStatusSnapshot;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.LogType;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ExecutionLogRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.MonitoredServerRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ServerStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ServerMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMonitorService.class);

    private static final String BASE_API_URL = "https://mcapi.us/server/status";

    private final ServerStatusRepository serverStatusRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final MonitoredServerRepository monitoredServerRepository;
    private final RestClient restClient;

    public ServerMonitorService(ServerStatusRepository serverStatusRepository, ExecutionLogRepository executionLogRepository, MonitoredServerRepository monitoredServerRepository) {
        this.serverStatusRepository = serverStatusRepository;
        this.executionLogRepository = executionLogRepository;
        this.monitoredServerRepository = monitoredServerRepository;
        this.restClient = RestClient.create();
    }

    @Scheduled(cron = "0 0/5 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    public void fetchServerStatus() {
        List<MonitoredServer> activeServers = monitoredServerRepository.findAllByActiveTrue();

        for(MonitoredServer server : activeServers) {
            if(server.getServerAddress() == null || server.getServerAddress().isBlank()) {
                continue;
            }

            processServer(server);
        }
    }

    private void processServer(MonitoredServer server) {
        LocalDateTime start = LocalDateTime.now();
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        String errorMessage = null;
        boolean isOnline;
        Integer onlinePlayers = null;

        try {
            String finalUrl = buildApiUrl(server.getServerAddress());

            ExternalServerStatusDTO externalStatus = restClient.get()
                    .uri(finalUrl)
                    .retrieve()
                    .body(ExternalServerStatusDTO.class);

            if(externalStatus != null) {
                isOnline = externalStatus.online();
                onlinePlayers = isOnline ? externalStatus.players().now() : 0;

                if(!isOnline && externalStatus.error() != null) {
                    errorMessage = externalStatus.error();
                }

                serverStatusRepository.save(ServerStatusSnapshot.builder()
                        .online(isOnline)
                        .onlinePlayers(onlinePlayers)
                        .timestamp(LocalDateTime.now())
                        .server(server)
                        .build());

                LOGGER.debug("[{}] Server status saved: {} (Players: {})", server.getName(), isOnline ? "Online" : "Offline", onlinePlayers);
            } else {
                status = ExecutionStatus.FAILURE;
                errorMessage = "API returned null body";
            }
        } catch(Exception exception) {
            LOGGER.error("[{}] Failed to fetch server status", server.getName(), exception);
            status = ExecutionStatus.FAILURE;
            errorMessage = exception.getMessage();

            serverStatusRepository.save(ServerStatusSnapshot.builder()
                    .onlinePlayers(0)
                    .online(false)
                    .timestamp(LocalDateTime.now())
                    .server(server)
                    .build());
        } finally {
            saveExecutionLog(server, start, status, errorMessage, onlinePlayers);
        }
    }

    private String buildApiUrl(String serverAddress) {
        String cleanAddress = serverAddress.trim();
        String ip;
        String port = "25565";

        if(cleanAddress.contains(":")) {
            String[] parts = cleanAddress.split(":");
            ip = parts[0];
            port = parts[1];
        } else {
            ip = cleanAddress;
        }

        return BASE_API_URL + "?ip=" + ip + "&port=" + port;
    }

    private void saveExecutionLog(MonitoredServer server, LocalDateTime start, ExecutionStatus status, String message, Integer onlinePlayers) {
        LocalDateTime end = LocalDateTime.now();

        if(message != null && message.length() > 900) {
            message = message.substring(0, 900) + "...";
        }

        ExecutionLog log = ExecutionLog.builder()
                .server(server)
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