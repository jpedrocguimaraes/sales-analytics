package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.model.*;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.*;
import br.com.fourzerofourdev.salesanalyticsbackend.service.crawler.SalesCrawlerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SalesMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesMonitorService.class);

    private final MonitoredServerRepository monitoredServerRepository;
    private final List<SalesCrawlerStrategy> strategies;
    private final Map<Long, Lock> serversLock = new ConcurrentHashMap<>();

    public SalesMonitorService(MonitoredServerRepository monitoredServerRepository, List<SalesCrawlerStrategy> strategies) {
        this.monitoredServerRepository = monitoredServerRepository;
        this.strategies = strategies;
    }

    @Scheduled(cron = "0/30 * * * * *")
    @EventListener(ApplicationReadyEvent.class)
    public void runCrawlers() {
        LOGGER.info("Starting Multi-Tenant Sales Monitor...");

        List<MonitoredServer> activeServers = monitoredServerRepository.findAllByActiveTrue();

        if(activeServers.isEmpty()) {
            LOGGER.info("No active servers configured.");
            return;
        }

        for(MonitoredServer server : activeServers) {
            Lock lock = serversLock.computeIfAbsent(server.getId(), k -> new ReentrantLock());

            if(lock.tryLock()) {
                try{
                    processServer(server);
                } finally {
                    lock.unlock();
                }
            } else {
                LOGGER.warn("Server {} is currently being processed. Skipping.", server.getName());
            }
        }
    }

    private void processServer(MonitoredServer server) {
        strategies.stream()
                .filter(strategy -> strategy.supports(server.getType()))
                .findFirst()
                .ifPresentOrElse(
                        strategy -> {
                            try {
                                strategy.execute(server);
                            } catch(Exception exception) {
                                LOGGER.error("Unexpected error executing strategy for server {}", server.getName(), exception);
                            }
                        },

                        () -> LOGGER.warn("No strategy found for server type: {}", server.getType())
                );
    }
}