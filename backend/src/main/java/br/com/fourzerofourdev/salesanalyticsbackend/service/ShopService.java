package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.CategoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.ProductDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.dto.ProductSaleHistoryDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.ProductCategory;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ServerType;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.MonitoredServerRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ProductCategoryRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.ProductRepository;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.SalesItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShopService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final MonitoredServerRepository monitoredServerRepository;
    private final SalesItemRepository salesItemRepository;

    public ShopService(ProductRepository productRepository, ProductCategoryRepository productCategoryRepository, MonitoredServerRepository monitoredServerRepository, SalesItemRepository salesItemRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.monitoredServerRepository = monitoredServerRepository;
        this.salesItemRepository = salesItemRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getShopOverview(Long serverId) {
        MonitoredServer server = monitoredServerRepository.findById(serverId).orElseThrow();

        List<ProductCategory> categories = productCategoryRepository.findByServerIdOrderByNameAsc(serverId);

        List<ProductDTO> products = productRepository.findProductsWithStats(serverId);

        Map<Long, List<ProductDTO>> productsByCategory = products.stream()
                .filter(product -> product.categoryId() != null)
                .map(product -> enrichLink(product, server))
                .collect(Collectors.groupingBy(ProductDTO::categoryId));

        List<CategoryDTO> result = new ArrayList<>();

        for(ProductCategory category : categories) {
            List<ProductDTO> categoryProducts = productsByCategory.getOrDefault(category.getId(), new ArrayList<>());

            double categoryTotal = categoryProducts.stream().mapToDouble(ProductDTO::totalRevenue).sum();

            result.add(new CategoryDTO(
                    category.getId(),
                    category.getName(),
                    categoryTotal,
                    categoryProducts
            ));
        }

        result.sort((a, b) -> Double.compare(b.totalCategoryRevenue(), a.totalCategoryRevenue()));

        return result;
    }

    @Transactional(readOnly = true)
    public Page<ProductSaleHistoryDTO> getProductHistory(Long serverId, Long productId, Pageable pageable) {
        Sort originalSort = pageable.getSort();
        Sort newSort = Sort.unsorted();

        if(originalSort.isSorted()) {
            for(Sort.Order order : originalSort) {
                String property = order.getProperty();
                Sort.Direction direction = order.getDirection();

                String jpqlProperty = switch(property) {
                    case "username", "player" -> "t.customer.username";
                    case "quantity" -> "i.quantity";
                    case "unitPrice" -> "i.unitPrice";
                    case "totalPrice" -> "(i.quantity * i.unitPrice)";
                    default -> "t.timestamp";
                };

                if(newSort.isUnsorted()) {
                    newSort = JpaSort.unsafe(direction, jpqlProperty);
                } else {
                    newSort = newSort.and(JpaSort.unsafe(direction, jpqlProperty));
                }
            }
        } else {
            newSort = JpaSort.unsafe(Sort.Direction.DESC, "t.timestamp");
        }

        Pageable newPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                newSort
        );

        return salesItemRepository.findSalesHistoryByProduct(productId, serverId, newPageable);
    }

    private ProductDTO enrichLink(ProductDTO product, MonitoredServer server) {
        String link = null;

        if(server.getType() == ServerType.HTML_RECENT_PAYMENTS && product.externalId() != null) {
            String baseUrl = server.getSalesUrl().endsWith("/")
                    ? server.getSalesUrl().substring(0, server.getSalesUrl().length() - 1)
                    : server.getSalesUrl();

            link = baseUrl + "/package/" + product.externalId();
        }

        return new ProductDTO(
                product.id(),
                product.name(),
                product.currentPrice(),
                product.externalId(),
                product.categoryId(),
                product.totalSalesCount(),
                product.totalRevenue(),
                link
        );
    }
}