package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerSummaryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerTransactionHistoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.CustomerRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.SalesTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return customerRepository.findAllCustomerSummaries(pageable);
    }

    @Transactional(readOnly = true)
    public List<CustomerTransactionHistoryDTO> getCustomerHistory(String username) {
        return salesTransactionRepository.findHistoryByUsername(username);
    }
}