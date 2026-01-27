package br.com.fourzerofourdev.salesanalyticsbackend.service.crawler;

import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ServerType;

public interface SalesCrawlerStrategy {
    boolean supports(ServerType type);
    void execute(MonitoredServer server);
}