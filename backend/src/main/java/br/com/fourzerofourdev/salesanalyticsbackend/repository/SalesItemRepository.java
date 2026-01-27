package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ProductSaleHistoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.SalesItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesItemRepository extends JpaRepository<SalesItem, Long> {

    @Query("""    
        SELECT new br.com.fourzerofourdev.salesanalyticsbackend.dto.ProductSaleHistoryDTO(
            t.customer.username,
            t.timestamp,
            i.quantity,
            i.unitPrice,
            CAST((i.quantity * i.unitPrice) AS double)
        )
        FROM SalesItem i
        JOIN i.transaction t
        WHERE i.product.id = :productId
        AND t.server.id = :serverId
    """)
    Page<ProductSaleHistoryDTO> findSalesHistoryByProduct(@Param("productId") Long productId, @Param("serverId") Long serverId, Pageable pageable);
}