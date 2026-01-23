package br.com.fourzerofourdev.salesanalyticsbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(unique = true, nullable = false)
    private UUID externalId;

    @Column(nullable = false)
    private String username;

    private LocalDateTime lastSeen;

    @PrePersist
    @PreUpdate
    public void updateLastSeen() {
        lastSeen = LocalDateTime.now();
    }
}