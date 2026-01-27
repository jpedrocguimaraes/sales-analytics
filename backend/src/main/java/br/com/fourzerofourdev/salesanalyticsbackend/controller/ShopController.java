package br.com.fourzerofourdev.salesanalyticsbackend.controller;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CategoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.service.ShopService;
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
}