package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerSummaryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerTransactionHistoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.TransactionItemDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.SalesTransaction;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.CustomerRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.SalesTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesTransactionRepository salesTransactionRepository;

    public CustomerService(CustomerRepository customerRepository, SalesTransactionRepository salesTransactionRepository) {
        this.customerRepository = customerRepository;
        this.salesTransactionRepository = salesTransactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<CustomerSummaryDTO> getAllCustomers(long serverId, Pageable pageable) {
        Sort originalSort = pageable.getSort();

        if(originalSort.isUnsorted()) {
            return customerRepository.findAllCustomerSummariesByServer(serverId, pageable);
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

        return customerRepository.findAllCustomerSummariesByServer(serverId, newPageable);
    }

    @Transactional(readOnly = true)
    public Page<CustomerTransactionHistoryDTO> getCustomerHistory(Long serverId, String username, Pageable pageable) {
        Sort originalSort = pageable.getSort();
        Sort newSort = Sort.unsorted();

        if(originalSort.isSorted()) {
            for(Sort.Order order : originalSort) {
                String property = order.getProperty();
                Sort.Direction direction = order.getDirection();

                String entityProperty = switch(property) {
                    case "amount", "value", "total" -> "amount";
                    default -> "timestamp";
                };

                if(newSort.isUnsorted()) {
                    newSort = Sort.by(direction, entityProperty);
                } else {
                    newSort = newSort.and(Sort.by(direction, entityProperty));
                }
            }
        } else {
            newSort = Sort.by(Sort.Direction.DESC, "timestamp");
        }

        Pageable newPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                newSort
        );

        Page<SalesTransaction> page = salesTransactionRepository.findHistoryByServerAndUsername(serverId, username, newPageable);

        return page.map(salesTransaction -> new CustomerTransactionHistoryDTO(
                salesTransaction.getId(),
                salesTransaction.getAmount(),
                salesTransaction.getTimestamp(),
                salesTransaction.getItems().stream().map(item -> new TransactionItemDTO(
                        item.getProductName(),
                        item.getQuantity(),
                        item.getProduct() != null ? item.getProduct().getId() : null
                )).toList()
        ));
    }
}