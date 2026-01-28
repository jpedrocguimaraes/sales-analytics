package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.GlobalSaleDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.TransactionItemDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.SalesTransaction;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.SalesTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService {

    private final SalesTransactionRepository salesTransactionRepository;

    public SalesService(SalesTransactionRepository salesTransactionRepository) {
        this.salesTransactionRepository = salesTransactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<GlobalSaleDTO> getAllSales(Long serverId, Pageable pageable) {
        Sort originalSort = pageable.getSort();
        Sort newSort = Sort.unsorted();

        if(originalSort.isSorted()) {
            for(Sort.Order order : originalSort) {
                String property = order.getProperty();
                Sort.Direction direction = order.getDirection();

                String entityProperty = switch (property) {
                    case "username", "player" -> "customer.username";
                    case "amount", "total" -> "amount";
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

        Page<SalesTransaction> page = salesTransactionRepository.findAllByServerId(serverId, newPageable);

        return page.map(salesTransaction -> new GlobalSaleDTO(
                salesTransaction.getId(),
                salesTransaction.getCustomer().getUsername(),
                salesTransaction.getTimestamp(),
                salesTransaction.getAmount(),
                salesTransaction.getItems().stream().map(salesItem -> new TransactionItemDTO(
                        salesItem.getProductName(),
                        salesItem.getQuantity(),
                        salesItem.getProduct() != null ? salesItem.getProduct().getId() : null
                )).toList()
        ));
    }
}