package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.*;
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
import java.util.*;

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

    public DashboardSummaryDTO getSummary(Long serverId, LocalDateTime start, LocalDateTime end) {
        Double totalValue = salesTransactionRepository.sumTotalBetween(serverId, start, end);
        BigDecimal total = BigDecimal.valueOf(totalValue != null ? totalValue : 0.0);

        long count = salesTransactionRepository.countByServerIdAndTimestampBetween(serverId, start, end);

        BigDecimal averageTicket = count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<TopDonorDTO> topDonors = salesTransactionRepository.findTopDonorsBetween(serverId, start, end);
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

        LocalDateTime lastSalesUpdate = leaderboardSnapshotRepository.findTopByServerIdOrderBySnapshotTimeDesc(serverId)
                .map(LeaderboardSnapshot::getSnapshotTime)
                .orElse(LocalDateTime.MIN);

        LocalDateTime lastServerUpdate = serverStatusRepository.findTopByServerIdOrderByTimestampDesc(serverId)
                .map(ServerStatusSnapshot::getTimestamp)
                .orElse(LocalDateTime.MIN);

        LocalDateTime lastUpdate;
        if(lastSalesUpdate.isAfter(lastServerUpdate)) {
            lastUpdate = lastSalesUpdate;
        } else {
            lastUpdate = lastServerUpdate;
        }

        if(lastUpdate.equals(LocalDateTime.MIN)) {
            lastUpdate = LocalDateTime.now();
        }

        return new DashboardSummaryDTO(total, averageTicket, count, topDonor, projection, lastUpdate);
    }

    public List<TopDonorDTO> getRanking(Long serverId, LocalDateTime start, LocalDateTime end) {
        return salesTransactionRepository.findTopDonorsBetween(serverId, start, end);
    }

    public ChartDataDTO getHourlySales(Long serverId, LocalDateTime start, LocalDateTime end) {
        List<SalesTransaction> transactions = salesTransactionRepository.findAllByServerIdAndTimestampBetweenOrderByTimestampAsc(serverId, start, end);

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

        List<ChartSeriesDTO> series = List.of(
                new ChartSeriesDTO("Vendas (R$)", data, "#4f46e5") // Indigo
        );

        return new ChartDataDTO(labels, series);
    }

    public Map<String, Long> getWhaleAnalysis(Long serverId, LocalDateTime start, LocalDateTime end) {
        List<TopDonorDTO> topDonors = salesTransactionRepository.findTopDonorsBetween(serverId, start, end);

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

    public ServerAnalyticsDTO getServerAnalytics(Long serverId, LocalDateTime start, LocalDateTime end) {
        Optional<ServerStatusSnapshot> peakOptional = serverStatusRepository.findPeakSnapshot(serverId, start, end);
        Optional<ServerStatusSnapshot> floorOptional = serverStatusRepository.findFloorSnapshot(serverId, start, end);
        Double averagePlayers = serverStatusRepository.findAveragePlayers(serverId, start, end);
        long totalSnapshots = serverStatusRepository.countByServerIdAndTimestampBetween(serverId, start, end);
        long onlineSnapshots = serverStatusRepository.countByServerIdAndOnlineTrueAndTimestampBetween(serverId, start, end);

        double uptime = 0.0;
        if(totalSnapshots > 0) {
            uptime = (double) onlineSnapshots / totalSnapshots * 100.0;
        }

        List<ServerStatusSnapshot> history = serverStatusRepository.findAllByServerIdAndTimestampBetweenOrderByTimestampAsc(serverId, start, end);

        List<String> labels = new ArrayList<>();
        List<Number> onlineData = new ArrayList<>();
        List<Number> offlineData = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        for(ServerStatusSnapshot snapshot : history) {
            labels.add(snapshot.getTimestamp().format(formatter));

            if(snapshot.isOnline()) {
                onlineData.add(snapshot.getOnlinePlayers());
                offlineData.add(null);
            } else {
                onlineData.add(null);
                offlineData.add(0);
            }
        }

        List<ChartSeriesDTO> series = new ArrayList<>();
        series.add(new ChartSeriesDTO("Online", onlineData, "#8b5cf6")); // Roxo
        series.add(new ChartSeriesDTO("Offline", offlineData, "#ef4444")); // Vermelho

        return new ServerAnalyticsDTO(
                peakOptional.map(ServerStatusSnapshot::getOnlinePlayers).orElse(0),
                floorOptional.map(ServerStatusSnapshot::getOnlinePlayers).orElse(0),
                peakOptional.map(serverStatusSnapshot -> serverStatusSnapshot.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"))).orElse("-"),
                averagePlayers != null ? averagePlayers : 0.0,
                uptime,
                new ChartDataDTO(labels, series)
        );
    }
}