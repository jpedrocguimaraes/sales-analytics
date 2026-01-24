package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerSummaryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUsername(String username);

    @Query(value = """
        SELECT new br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerSummaryDTO(
            c.username,
            CAST(COALESCE(SUM(t.amount), 0.0) AS double),
            COUNT(t.id),
            MAX(t.timestamp)
        )
        FROM Customer c
        LEFT JOIN SalesTransaction t ON t.customer = c
        GROUP BY c.username
    """,
    countQuery = "SELECT COUNT(c) FROM Customer c")
    Page<CustomerSummaryDTO> findAllCustomerSummaries(Pageable pageable);
}