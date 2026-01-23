package br.com.fourzerofourdev.salesanalyticsbackend.controller;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ChartDataDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.DashboardSummaryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.TopDonorDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryDTO getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return dashboardService.getSummary(resolveStart(start), resolveEnd(end));
    }

    @GetMapping("/ranking")
    public List<TopDonorDTO> getRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return dashboardService.getRanking(resolveStart(start), resolveEnd(end));
    }

    @GetMapping("/chart/hourly")
    public ChartDataDTO getHourlyChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return dashboardService.getHourlySales(resolveStart(start), resolveEnd(end));
    }

    @GetMapping("/whales")
    public Map<String, Long> getWhales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return dashboardService.getWhaleAnalysis(resolveStart(start), resolveEnd(end));
    }

    private LocalDateTime resolveStart(LocalDateTime date) {
        if(date != null) return date;
        return LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
    }

    private LocalDateTime resolveEnd(LocalDateTime date) {
        if(date != null) return date;
        return LocalDateTime.now();
    }
}