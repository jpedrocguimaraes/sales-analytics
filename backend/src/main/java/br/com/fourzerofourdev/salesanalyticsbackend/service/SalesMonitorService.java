package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.PlayerDTO;
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

    @Scheduled(fixedDelay = 600000, initialDelay = 5000)
    @Transactional
    public void fetchAndProcessLeaderboard() {
        LOGGER.info("Fetching leaderboard from {}", targetUrl);

        try {
            List<PlayerDTO> players = restClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if(players == null || players.isEmpty()) {
                LOGGER.warn("No players found in leaderboard");
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            for(PlayerDTO player : players) {
                processPlayer(player, now);
            }

            LOGGER.info("Leaderboard processed successfully. Total players: {}", players.size());
        } catch(Exception exception) {
            LOGGER.error("Error processing leaderboard", exception);
        }
    }

    private void processPlayer(PlayerDTO player, LocalDateTime now) {
        Customer customer = customerRepository.findByUsername(player.username())
                .orElseGet(() -> {
                    LOGGER.info("New customer found: {}", player.username());

                    return customerRepository.save(Customer.builder()
                            .username(player.username())
                            .lastExternalId(player.id())
                            .lastSeen(now)
                            .build());
                });

        if(!customer.getUsername().equals(player.username())) {
            customer.setUsername(player.username());
        }

        customer.setLastSeen(now);
        customerRepository.save(customer);

        Optional<LeaderboardSnapshot> lastSnapshotOptional = leaderboardSnapshotRepository.findTopByCustomerOrderBySnapshotTimeDesc(customer);

        double currentTotal = player.total();
        double previousTotal = lastSnapshotOptional.map(LeaderboardSnapshot::getTotalAccumulated).orElse(0.0);

        if(lastSnapshotOptional.isPresent()) {
            double delta = currentTotal - previousTotal;

            if(delta > 0.01) {
                LOGGER.info("New sales for customer {}: {}", customer.getUsername(), delta);

                salesTransactionRepository.save(SalesTransaction.builder()
                        .customer(customer)
                        .amount(delta)
                        .timestamp(now)
                        .build());
            } else if(delta < 0) {
                LOGGER.warn("Negative sales for customer {}: {}", customer.getUsername(), delta);
            }
        } else {
            LOGGER.info("New customer {} with total sales: {}", customer.getUsername(), currentTotal);
        }

        leaderboardSnapshotRepository.save(LeaderboardSnapshot.builder()
                .customer(customer)
                .totalAccumulated(currentTotal)
                .snapshotTime(now)
                .build());
    }
}