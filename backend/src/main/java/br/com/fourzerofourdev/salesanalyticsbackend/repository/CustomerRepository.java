package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerSummaryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.Customer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUsernameAndServer(String username, MonitoredServer server);

    @Query(value = """
        SELECT new br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerSummaryDTO(
            c.username,
            CAST(COALESCE(SUM(t.amount), 0.0) AS double),
            COUNT(t.id),
            MAX(t.timestamp)
        )
        FROM Customer c
        LEFT JOIN SalesTransaction t ON t.customer = c
        WHERE c.server.id = :serverId
        GROUP BY c.username
    """,
    countQuery = "SELECT COUNT(c) FROM Customer c WHERE c.server.id = :serverId")
    Page<CustomerSummaryDTO> findAllCustomerSummariesByServer(@Param("serverId") Long serverId, Pageable pageable);
}