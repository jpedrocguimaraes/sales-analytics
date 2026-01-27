package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByExternalIdAndServer(Long externalId, MonitoredServer server);
    Optional<Product> findByNameAndServer(String name, MonitoredServer server);
}