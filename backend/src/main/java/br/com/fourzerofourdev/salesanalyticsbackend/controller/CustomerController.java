package br.com.fourzerofourdev.salesanalyticsbackend.controller;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerSummaryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.CustomerTransactionHistoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "http://localhost:4200")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public Page<CustomerSummaryDTO> getAllCustomers(@PageableDefault(sort = "username") Pageable pageable) {
        return customerService.getAllCustomers(pageable);
    }

    @GetMapping("/{username}/history")
    public List<CustomerTransactionHistoryDTO> getCustomerHistory(@PathVariable String username) {
        return customerService.getCustomerHistory(username);
    }
}