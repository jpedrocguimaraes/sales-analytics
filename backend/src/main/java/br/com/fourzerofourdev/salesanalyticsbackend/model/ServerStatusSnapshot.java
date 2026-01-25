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

    private int onlinePlayers;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}