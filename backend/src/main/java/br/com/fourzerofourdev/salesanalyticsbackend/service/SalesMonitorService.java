package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ExternalCustomerDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.Customer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.LeaderboardSnapshot;
import br.com.fourzerofourdev.salesanalyticsbackend.model.SalesTransaction;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.CustomerRepository;
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
import java.util.List;
import java.util.Optional;

@Service
public class SalesMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesMonitorService.class);

    private final CustomerRepository customerRepository;
    private final LeaderboardSnapshotRepository leaderboardSnapshotRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final RestClient restClient;

    @Value("${app.crawler.target-url}")
    private String targetUrl;

    public SalesMonitorService(CustomerRepository customerRepository, LeaderboardSnapshotRepository leaderboardSnapshotRepository, SalesTransactionRepository salesTransactionRepository) {
        this.customerRepository = customerRepository;
        this.leaderboardSnapshotRepository = leaderboardSnapshotRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.restClient = RestClient.create();
    }

    @Scheduled(cron = "0 0/5 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void fetchAndProcessSalesData() {
        LOGGER.info("Fetching customer data from {}", targetUrl);

        try {
            List<ExternalCustomerDTO> customers = restClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if(customers == null || customers.isEmpty()) {
                LOGGER.warn("No customers found in leaderboard");
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            for(ExternalCustomerDTO customer : customers) {
                processCustomer(customer, now);
            }

            LOGGER.info("Sales data processed successfully. Total customers: {}", customers.size());
        } catch(Exception exception) {
            LOGGER.error("Error processing sales data", exception);
        }
    }

    private void processCustomer(ExternalCustomerDTO customerDTO, LocalDateTime now) {
        Customer customer = customerRepository.findByUsername(customerDTO.username())
                .orElseGet(() -> {
                    LOGGER.info("New customer found: {}", customerDTO.username());

                    return customerRepository.save(Customer.builder()
                            .username(customerDTO.username())
                            .lastExternalId(customerDTO.id())
                            .lastSeen(now)
                            .build());
                });

        if(!customer.getUsername().equals(customerDTO.username())) {
            customer.setUsername(customerDTO.username());
        }

        customer.setLastSeen(now);
        customerRepository.save(customer);

        Optional<LeaderboardSnapshot> lastSnapshotOptional = leaderboardSnapshotRepository.findTopByCustomerOrderBySnapshotTimeDesc(customer);

        double currentTotal = customerDTO.total();
        double previousTotal = lastSnapshotOptional.map(LeaderboardSnapshot::getTotalAccumulated).orElse(0.0);

        if(lastSnapshotOptional.isPresent()) {
            double delta = currentTotal - previousTotal;

            double tolerance = 0.01;

            if(delta > tolerance) {
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
    }
}