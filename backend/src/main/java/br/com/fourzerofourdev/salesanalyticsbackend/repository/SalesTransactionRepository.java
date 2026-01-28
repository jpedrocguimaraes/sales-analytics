package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.TopDonorDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.SalesTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {

    @Query("SELECT SUM(t.amount) FROM SalesTransaction t WHERE t.server.id = :serverId AND t.timestamp BETWEEN :start AND :end")
    Double sumTotalBetween(@Param("serverId") Long serverId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByServerIdAndTimestampBetween(Long serverId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT new br.com.fourzerofourdev.salesanalyticsbackend.dto.TopDonorDTO(c.username, SUM(t.amount))
        FROM SalesTransaction t
        JOIN t.customer c
        WHERE t.server.id = :serverId AND t.timestamp BETWEEN :start AND :end
        GROUP BY c.username
        ORDER BY SUM(t.amount) DESC
    """)
    List<TopDonorDTO> findTopDonorsBetween(@Param("serverId") Long serverId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<SalesTransaction> findAllByServerIdAndTimestampBetweenOrderByTimestampAsc(Long serverId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT t FROM SalesTransaction t
        WHERE t.customer.username = :username
        AND t.server.id = :serverId
    """)
    Page<SalesTransaction> findHistoryByServerAndUsername(@Param("serverId") Long serverId, @Param("username") String username, Pageable pageable);
}