package br.com.fourzerofourdev.salesanalyticsbackend.controller;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ExecutionLogDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.LogType;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ExecutionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-logs")
@CrossOrigin(origins = "http://localhost:4200")
public class SystemLogController {

    private final ExecutionLogRepository executionLogRepository;

    public SystemLogController(ExecutionLogRepository executionLogRepository) {
        this.executionLogRepository = executionLogRepository;
    }

    @GetMapping
    public Page<ExecutionLogDTO> getLogs(@RequestParam Long serverId, @RequestParam(required = false) LogType type, @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return executionLogRepository.findLogsByServerIdAndType(serverId, type, pageable)
                .map(log -> new ExecutionLogDTO(
                        log.getId(),
                        log.getStartTime(),
                        log.getDurationMs(),
                        log.getStatus(),
                        log.getType(),
                        log.getNewCustomersCount(),
                        log.getNewSalesCount(),
                        log.getOnlinePlayersCount(),
                        log.getMessage()
                ));
    }
}