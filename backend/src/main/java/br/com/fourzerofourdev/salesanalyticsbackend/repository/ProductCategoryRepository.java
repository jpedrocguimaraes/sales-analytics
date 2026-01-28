package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findByExternalIdAndServer(Long externalId, MonitoredServer server);

    Optional<ProductCategory> findByNameAndServer(String name, MonitoredServer server);

    @Query("SELECT c FROM ProductCategory c WHERE c.server.id = :serverId AND (:active IS NULL OR c.active = :active) ORDER BY c.name ASC")
    List<ProductCategory> findByServerIdAndStatus(@Param("serverId") Long serverId, @Param("active") Boolean active);

    @Modifying
    @Transactional
    @Query("UPDATE ProductCategory c SET c.active = false WHERE c.server.id = :serverId AND c.lastScrapedAt < :threshold")
    void markCategoriesAsInactiveIfOlderThan(@Param("serverId") Long serverId, @Param("threshold") LocalDateTime threshold);
}