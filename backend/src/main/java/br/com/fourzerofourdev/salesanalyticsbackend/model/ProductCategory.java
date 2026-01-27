package br.com.fourzerofourdev.salesanalyticsbackend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "server_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private Long externalId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private MonitoredServer server;
}