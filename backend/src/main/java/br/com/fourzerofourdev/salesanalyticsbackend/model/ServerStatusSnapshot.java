package br.com.fourzerofourdev.salesanalyticsbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "server_status_snapshots")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ServerStatusSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private MonitoredServer server;

    @Column(nullable = false)
    private boolean online;

    private int onlinePlayers;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}