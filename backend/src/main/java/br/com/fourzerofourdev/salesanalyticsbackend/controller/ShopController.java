package br.com.fourzerofourdev.salesanalyticsbackend.controller;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CategoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.ProductPriceHistoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.ProductSaleHistoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@CrossOrigin(origins = "http://localhost:4200")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public List<CategoryDTO> getShopOverview(@RequestParam Long serverId) {
        return shopService.getShopOverview(serverId);
    }

    @GetMapping("/products/{productId}/history")
    public Page<ProductSaleHistoryDTO> getProductHistory(@PathVariable Long productId, @RequestParam Long serverId, @PageableDefault(sort = "transaction.timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return shopService.getProductHistory(serverId, productId, pageable);
    }

    @GetMapping("/products/{productId}/price-history")
    public List<ProductPriceHistoryDTO> getProductPriceHistory(@PathVariable Long productId) {
        return shopService.getProductPriceHistory(productId);
    }
}