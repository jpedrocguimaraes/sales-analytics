package br.com.fourzerofourdev.salesanalyticsbackend.controller;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ExecutionLogDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ExecutionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-logs")
@CrossOrigin(origins = "http://localhost:4200")
public class SystemLogController {

    private final ExecutionLogRepository executionLogRepository;

    public SystemLogController(ExecutionLogRepository executionLogRepository) {
        this.executionLogRepository = executionLogRepository;
    }

    @GetMapping
    public Page<ExecutionLogDTO> getLogs(@PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return executionLogRepository.findAllLogs(pageable)
                .map(log -> new ExecutionLogDTO(
                        log.getId(),
                        log.getStartTime(),
                        log.getDurationMs(),
                        log.getStatus(),
                        log.getNewCustomersCount(),
                        log.getNewSalesCount(),
                        log.getMessage()
                ));
    }
}