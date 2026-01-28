package br.com.fourzerofourdev.salesanalyticsbackend.controller;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.GlobalSaleDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.service.SalesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "http://localhost:4200")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping
    public Page<GlobalSaleDTO> getAllSales(@RequestParam Long serverId, @PageableDefault(size = 20) Pageable pageable) {
        return salesService.getAllSales(serverId, pageable);
    }
}