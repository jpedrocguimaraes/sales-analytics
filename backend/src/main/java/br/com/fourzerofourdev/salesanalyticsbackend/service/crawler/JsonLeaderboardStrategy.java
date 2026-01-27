package br.com.fourzerofourdev.salesanalyticsbackend.service.crawler;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ExternalCustomerDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.*;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ServerType;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.CustomerRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ExecutionLogRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.LeaderboardSnapshotRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.SalesTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class JsonLeaderboardStrategy extends AbstractSalesCrawlerStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLeaderboardStrategy.class);

    private final CustomerRepository customerRepository;
    private final LeaderboardSnapshotRepository leaderboardSnapshotRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final RestClient restClient;

    public JsonLeaderboardStrategy(CustomerRepository customerRepository, LeaderboardSnapshotRepository leaderboardSnapshotRepository, SalesTransactionRepository salesTransactionRepository, ExecutionLogRepository executionLogRepository) {
        super(executionLogRepository);
        this.customerRepository = customerRepository;
        this.leaderboardSnapshotRepository = leaderboardSnapshotRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.restClient = RestClient.create();
    }

    private record ProcessResult(boolean isNewCustomer, boolean isNewSale) {}

    @Override
    public boolean supports(ServerType type) {
        return type == ServerType.JSON_LEADERBOARD;
    }

    @Override
    public void execute(MonitoredServer server) {
        LOGGER.info("[{}] Starting JSON crawler...", server.getName());
        LocalDateTime start = LocalDateTime.now();

        int newCustomers = 0;
        int newSales = 0;
        int processingErrors = 0;
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        String errorMessage = null;

        try {
            List<ExternalCustomerDTO> customers = restClient.get()
                    .uri(server.getSalesUrl())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if(customers == null || customers.isEmpty()) {
                LOGGER.warn("[{}] No customers found in API response", server.getName());
                errorMessage = "API returned empty list";
            } else {
                LocalDateTime now = LocalDateTime.now();

                for(ExternalCustomerDTO customer : customers) {
                    try {
                        ProcessResult result = processCustomer(customer, server, now);
                        if(result.isNewCustomer) newCustomers++;
                        if(result.isNewSale) newSales++;
                    } catch(Exception exception) {
                        processingErrors++;
                        LOGGER.error("[{}] Error processing customer {}: {}", server.getName(), customer.username(), exception.getMessage());
                    }
                }
            }

            if(processingErrors > 0) {
                if(processingErrors == customers.size()) {
                    status = ExecutionStatus.FAILURE;
                    errorMessage = "All records failed to process";
                } else {
                    status = ExecutionStatus.PARTIAL_FAILURE;
                    errorMessage = processingErrors + " records failed out of " + customers.size();
                }
            }
        } catch(Exception exception) {
            LOGGER.error("[{}] Critical error crawling API", server.getName(), exception);
            status = ExecutionStatus.FAILURE;
            errorMessage = exception.getMessage();
        } finally {
            saveLog(server, start, status, newCustomers, newSales, errorMessage);
        }
    }

    private ProcessResult processCustomer(ExternalCustomerDTO customerDTO, MonitoredServer server, LocalDateTime now) {
        boolean isNewCustomer = false;
        boolean isNewSale = false;

        Customer customer = customerRepository.findByUsernameAndServer(customerDTO.username(), server)
                .orElse(null);

        if(customer == null) {
            isNewCustomer = true;

            customer = customerRepository.save(Customer.builder()
                    .username(customerDTO.username())
                    .lastExternalId(customerDTO.id())
                    .server(server)
                    .lastSeen(now)
                    .build());

            LOGGER.info("[{}] New customer found: {}", server.getName(), customerDTO.username());
        } else {
            if(!customer.getUsername().equals(customerDTO.username())) {
                customer.setUsername(customerDTO.username());
            }

            customer.setLastSeen(now);
            customerRepository.save(customer);
        }

        Optional<LeaderboardSnapshot> lastSnapshotOptional = leaderboardSnapshotRepository.findTopByCustomerOrderBySnapshotTimeDesc(customer);

        double currentTotal = customerDTO.total();
        double previousTotal = lastSnapshotOptional.map(LeaderboardSnapshot::getTotalAccumulated).orElse(0.0);
        double delta = currentTotal - previousTotal;
        double tolerance = 0.01;

        if(lastSnapshotOptional.isPresent()) {
            if(delta > tolerance) {
                isNewSale = true;

                salesTransactionRepository.save(SalesTransaction.builder()
                        .customer(customer)
                        .amount(delta)
                        .timestamp(now)
                        .server(server)
                        .build());

                LOGGER.info("[{}] New sale: {} (+{})", server.getName(), customer.getUsername(), delta);
            } else if(delta < -tolerance) {
                LOGGER.info("[{}] Reset detected: {}", server.getName(), customer.getUsername());
            }
        } else {
            LOGGER.info("[{}] First snapshot for {}. History: {}", server.getName(), customer.getUsername(), currentTotal);
        }

        boolean hasChanged = Math.abs(delta) > tolerance;
        boolean isFirstSnapshot = lastSnapshotOptional.isEmpty();
        boolean isHeartbeatNeeded = false;

        if(lastSnapshotOptional.isPresent()) {
            isHeartbeatNeeded = lastSnapshotOptional.get().getSnapshotTime().isBefore(now.minusHours(6));
        }

        if(hasChanged || isFirstSnapshot || isHeartbeatNeeded) {
            leaderboardSnapshotRepository.save(LeaderboardSnapshot.builder()
                    .customer(customer)
                    .totalAccumulated(currentTotal)
                    .snapshotTime(now)
                    .server(server)
                    .build());
        }

        return new ProcessResult(isNewCustomer, isNewSale);
    }
}