package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ExternalCustomerDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.Customer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ExecutionLog;
import br.com.fourzerofourdev.salesanalyticsbackend.model.LeaderboardSnapshot;
import br.com.fourzerofourdev.salesanalyticsbackend.model.SalesTransaction;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.CustomerRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ExecutionLogRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.LeaderboardSnapshotRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.SalesTransactionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class SalesMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesMonitorService.class);

    private final CustomerRepository customerRepository;
    private final LeaderboardSnapshotRepository leaderboardSnapshotRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final RestClient restClient;

    @Value("${app.crawler.target-url}")
    private String targetUrl;

    public SalesMonitorService(CustomerRepository customerRepository, LeaderboardSnapshotRepository leaderboardSnapshotRepository, SalesTransactionRepository salesTransactionRepository, ExecutionLogRepository executionLogRepository) {
        this.customerRepository = customerRepository;
        this.leaderboardSnapshotRepository = leaderboardSnapshotRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.executionLogRepository = executionLogRepository;
        this.restClient = RestClient.create();
    }

    private record ProcessResult(boolean isNewCustomer, boolean isNewSale) {}

    @Scheduled(cron = "0 0/5 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void fetchAndProcessSalesData() {
        LOGGER.info("Starting scheduled execution...");
        LocalDateTime start = LocalDateTime.now();

        int newCustomers = 0;
        int newSales = 0;
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        String errorMessage = null;

        try {
            List<ExternalCustomerDTO> customers = restClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if(customers == null || customers.isEmpty()) {
                LOGGER.warn("No customers found.");
                errorMessage = "API returned empty list";
            } else {
                LocalDateTime now = LocalDateTime.now();

                for(ExternalCustomerDTO customer : customers) {
                    ProcessResult result = processCustomer(customer, now);
                    if(result.isNewCustomer) newCustomers++;
                    if(result.isNewSale) newSales++;
                }

                LOGGER.info("Execution finished. New Customers: {}, New Sales: {}", newCustomers, newSales);
            }
        } catch(Exception exception) {
            LOGGER.error("Critical error during execution", exception);
            status = ExecutionStatus.FAILURE;
            errorMessage = exception.getMessage();
            if(errorMessage != null && errorMessage.length() > 900) {
                errorMessage = errorMessage.substring(0, 900) + "...";
            }
        } finally {
            LocalDateTime end = LocalDateTime.now();
            long duration = ChronoUnit.MILLIS.between(start, end);

            ExecutionLog log = ExecutionLog.builder()
                    .startTime(start)
                    .endTime(end)
                    .durationMs(duration)
                    .status(status)
                    .newCustomersCount(newCustomers)
                    .newSalesCount(newSales)
                    .message(errorMessage)
                    .build();

            executionLogRepository.save(log);
        }
    }

    private ProcessResult processCustomer(ExternalCustomerDTO customerDTO, LocalDateTime now) {
        boolean isNewCustomer = false;
        boolean isNewSale = false;

        Customer customer = customerRepository.findByUsername(customerDTO.username())
                .orElse(null);

        if(customer == null) {
            isNewCustomer = true;
            LOGGER.info("New customer found: {}", customerDTO.username());

            customer = customerRepository.save(Customer.builder()
                    .username(customerDTO.username())
                    .lastExternalId(customerDTO.id())
                    .lastSeen(now)
                    .build());
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

        if(lastSnapshotOptional.isPresent()) {
            double delta = currentTotal - previousTotal;
            double tolerance = 0.01;

            if(delta > tolerance) {
                isNewSale = true;
                LOGGER.info("New sales for customer {}: {}", customer.getUsername(), delta);

                salesTransactionRepository.save(SalesTransaction.builder()
                        .customer(customer)
                        .amount(delta)
                        .timestamp(now)
                        .build());
            } else if(delta < -tolerance) {
                LOGGER.warn("Negative sales for customer {}: {}", customer.getUsername(), delta);
            }
        } else {
            LOGGER.info("First snapshot for customer {}. Total history: {}", customer.getUsername(), currentTotal);
        }

        leaderboardSnapshotRepository.save(LeaderboardSnapshot.builder()
                .customer(customer)
                .totalAccumulated(currentTotal)
                .snapshotTime(now)
                .build());

        return new ProcessResult(isNewCustomer, isNewSale);
    }
}