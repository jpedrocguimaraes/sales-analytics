package br.com.fourzerofourdev.salesanalyticsbackend.model;

import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
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

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    private int newCustomersCount;

    private int newSalesCount;

    @Column(columnDefinition = "TEXT", length = 1000)
    private String message;
}