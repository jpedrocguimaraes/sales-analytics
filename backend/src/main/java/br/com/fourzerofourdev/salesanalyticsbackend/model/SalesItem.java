package br.com.fourzerofourdev.salesanalyticsbackend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sales_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SalesItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private SalesTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Double unitPrice;

    public Double getTotalPrice() {
        return unitPrice * quantity;
    }
}