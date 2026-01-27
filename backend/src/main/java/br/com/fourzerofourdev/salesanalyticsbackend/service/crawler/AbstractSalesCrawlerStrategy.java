package br.com.fourzerofourdev.salesanalyticsbackend.service.crawler;

import br.com.fourzerofourdev.salesanalyticsbackend.model.ExecutionLog;
import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.LogType;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ExecutionLogRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public abstract class AbstractSalesCrawlerStrategy implements SalesCrawlerStrategy {

    protected final ExecutionLogRepository executionLogRepository;

    protected AbstractSalesCrawlerStrategy(ExecutionLogRepository executionLogRepository) {
        this.executionLogRepository = executionLogRepository;
    }

    protected void saveLog(MonitoredServer server, LocalDateTime start, ExecutionStatus status, int newCustomers, int newSales, String errorMessage) {
        if(errorMessage != null && errorMessage.length() > 900) {
            errorMessage = errorMessage.substring(0, 900) + "...";
        }

        ExecutionLog log = ExecutionLog.builder()
                .server(server)
                .startTime(start)
                .endTime(LocalDateTime.now())
                .type(LogType.SALES_CRAWLER)
                .durationMs(ChronoUnit.MILLIS.between(start, LocalDateTime.now()))
                .status(status)
                .newCustomersCount(newCustomers)
                .newSalesCount(newSales)
                .message(errorMessage)
                .build();

        executionLogRepository.save(log);
    }
}