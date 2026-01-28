package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ProductDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByExternalIdAndServer(Long externalId, MonitoredServer server);

    Optional<Product> findByNameAndServer(String name, MonitoredServer server);

    @Query("""
        SELECT new br.com.fourzerofourdev.salesanalyticsbackend.dto.ProductDTO(
            p.id,
            p.name,
            p.currentPrice,
            p.externalId,
            p.category.id,
            CAST(COALESCE(SUM(si.quantity), 0) AS long),
            CAST(COALESCE(SUM(si.unitPrice * si.quantity), 0.0) AS double),
            '',
            p.active
        )
        FROM Product p
        LEFT JOIN SalesItem si ON si.product = p
        WHERE p.server.id = :serverId
        GROUP BY p.id, p.name, p.currentPrice, p.externalId, p.category.id, p.active
        ORDER BY COALESCE(SUM(si.unitPrice * si.quantity), 0.0) DESC
    """)
    List<ProductDTO> findProductsWithStats(@Param("serverId") Long serverId);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.active = false WHERE p.server.id = :serverId AND p.lastScrapedAt < :threshold")
    void markProductsAsInactiveIfOlderThan(@Param("serverId") Long serverId, @Param("threshold") LocalDateTime threshold);
}