package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ChartDataDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.DashboardSummaryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.ServerAnalyticsDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.TopDonorDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.LeaderboardSnapshot;
import br.com.fourzerofourdev.salesanalyticsbackend.model.SalesTransaction;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ServerStatusSnapshot;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.LeaderboardSnapshotRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.SalesTransactionRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ServerStatusRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final LeaderboardSnapshotRepository leaderboardSnapshotRepository;
    private final ServerStatusRepository serverStatusRepository;

    public DashboardService(SalesTransactionRepository salesTransactionRepository, LeaderboardSnapshotRepository leaderboardSnapshotRepository, ServerStatusRepository serverStatusRepository) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.leaderboardSnapshotRepository = leaderboardSnapshotRepository;
        this.serverStatusRepository = serverStatusRepository;
    }

    public DashboardSummaryDTO getSummary(LocalDateTime start, LocalDateTime end) {
        Double totalValue = salesTransactionRepository.sumTotalBetween(start, end);
        BigDecimal total = BigDecimal.valueOf(totalValue != null ? totalValue : 0.0);

        long count = salesTransactionRepository.countByTimestampBetween(start, end);

        BigDecimal averageTicket = count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<TopDonorDTO> topDonors = salesTransactionRepository.findTopDonorsBetween(start, end);
        TopDonorDTO topDonor = topDonors.isEmpty() ? null : topDonors.getFirst();

        BigDecimal projection = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        if(end.isAfter(now) && start.isBefore(now)) {
            long totalSecondsInPeriod = Duration.between(start, end).getSeconds();
            long elapsedSeconds = Duration.between(start, now).getSeconds();

            if(elapsedSeconds > 0) {
                projection = total
                        .divide(BigDecimal.valueOf(elapsedSeconds), 10, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(totalSecondsInPeriod))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        LocalDateTime lastUpdate = leaderboardSnapshotRepository.findTopByOrderBySnapshotTimeDesc()
                .map(LeaderboardSnapshot::getSnapshotTime)
                .orElse(LocalDateTime.now());

        return new DashboardSummaryDTO(total, averageTicket, count, topDonor, projection, lastUpdate);
    }

    public List<TopDonorDTO> getRanking(LocalDateTime start, LocalDateTime end) {
        return salesTransactionRepository.findTopDonorsBetween(start, end);
    }

    public ChartDataDTO getHourlySales(LocalDateTime start, LocalDateTime end) {
        List<SalesTransaction> transactions = salesTransactionRepository.findAllByTimestampBetweenOrderByTimestampAsc(start, end);

        double[] hourlySums = new double[24];

        for(SalesTransaction transaction : transactions) {
            int hour = transaction.getTimestamp().getHour();
            hourlySums[hour] += transaction.getAmount();
        }

        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();

        for(int i = 0; i < 24; i++) {
            labels.add(String.format("%02d:00", i));
            data.add(hourlySums[i]);
        }

        return new ChartDataDTO(labels, data);
    }

    public Map<String, Long> getWhaleAnalysis(LocalDateTime start, LocalDateTime end) {
        List<TopDonorDTO> topDonors = salesTransactionRepository.findTopDonorsBetween(start, end);

        long whales = 0;
        long dolphins = 0;
        long fish = 0;

        for(TopDonorDTO topDonor : topDonors) {
            double value = topDonor.total() != null ? topDonor.total() : 0.0;
            if(value >= 2500) whales++;
            else if(value >= 1000) dolphins++;
            else fish++;
        }

        Map<String, Long> result = new HashMap<>();
        result.put("Whales (>2.5k)", whales);
        result.put("Dolphins (>1k)", dolphins);
        result.put("Minnows (<1k)", fish);

        return result;
    }

    public ServerAnalyticsDTO getServerAnalytics(LocalDateTime start, LocalDateTime end) {
        ServerStatusSnapshot peak = serverStatusRepository.findPeakSnapshot(start, end);
        ServerStatusSnapshot floor = serverStatusRepository.findFloorSnapshot(start, end);
        Double averagePlayers = serverStatusRepository.findAveragePlayers(start, end);
        List<ServerStatusSnapshot> history = serverStatusRepository.findAllByTimestampBetweenOrderByTimestampAsc(start, end);

        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        for(ServerStatusSnapshot snapshot : history) {
            labels.add(snapshot.getTimestamp().format(formatter));
            data.add(snapshot.getOnlinePlayers());
        }

        return new ServerAnalyticsDTO(
                peak != null ? peak.getOnlinePlayers() : 0,
                floor != null ? floor.getOnlinePlayers() : 0,
                peak != null ? peak.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")) : "-",
                averagePlayers != null ? averagePlayers : 0.0,
                new ChartDataDTO(labels, data)
        );
    }
}