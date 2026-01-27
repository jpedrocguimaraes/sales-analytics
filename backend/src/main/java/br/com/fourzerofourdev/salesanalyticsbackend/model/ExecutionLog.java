package br.com.fourzerofourdev.salesanalyticsbackend.model;

import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.LogType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private MonitoredServer server;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    @Enumerated(EnumType.STRING)
    private LogType type;

    private Integer newCustomersCount;

    private Integer newSalesCount;

    private Integer onlinePlayersCount;

    @Column(columnDefinition = "TEXT", length = 1000)
    private String message;
}