package br.com.fourzerofourdev.salesanalyticsbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private MonitoredServer server;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Double amount;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SalesItem> items = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public void addItem(SalesItem item) {
        items.add(item);
        item.setTransaction(this);
    }
}