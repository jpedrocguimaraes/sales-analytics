package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    Optional<ProductCategory> findByExternalIdAndServer(Long externalId, MonitoredServer server);
    Optional<ProductCategory> findByNameAndServer(String name, MonitoredServer server);
}