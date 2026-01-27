package br.com.fourzerofourdev.salesanalyticsbackend.model;

import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ServerType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "monitored_servers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MonitoredServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerType type;

    @Column(nullable = false)
    private String salesUrl;

    private String serverAddress;

    private boolean active;

    @Column(columnDefinition = "TEXT")
    private String lastCrawledSignatures;
}