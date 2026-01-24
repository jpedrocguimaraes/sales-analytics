package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerSummaryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerTransactionHistoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.CustomerRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.SalesTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesTransactionRepository salesTransactionRepository;

    public CustomerService(CustomerRepository customerRepository, SalesTransactionRepository salesTransactionRepository) {
        this.customerRepository = customerRepository;
        this.salesTransactionRepository = salesTransactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<CustomerSummaryDTO> getAllCustomers(Pageable pageable) {
        Sort originalSort = pageable.getSort();

        if(originalSort.isUnsorted()) {
            return customerRepository.findAllCustomerSummaries(pageable);
        }

        Sort.Order order = originalSort.iterator().next();
        String property = order.getProperty();
        Sort.Direction direction = order.getDirection();

        String jpqlProperty = switch(property) {
            case "totalSpent" -> "COALESCE(SUM(t.amount), 0)";
            case "purchaseCount" -> "COUNT(t.id)";
            case "lastPurchaseDate" -> "MAX(t.timestamp)";
            default -> "c.username";
        };

        Pageable newPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                JpaSort.unsafe(direction, jpqlProperty)
        );

        return customerRepository.findAllCustomerSummaries(newPageable);
    }

    @Transactional(readOnly = true)
    public List<CustomerTransactionHistoryDTO> getCustomerHistory(String username) {
        return salesTransactionRepository.findHistoryByUsername(username);
    }
}