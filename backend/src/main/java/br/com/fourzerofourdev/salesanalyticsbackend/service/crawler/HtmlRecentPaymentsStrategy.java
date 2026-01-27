package br.com.fourzerofourdev.salesanalyticsbackend.service.crawler;

import br.com.fourzerofourdev.salesanalyticsbackend.model.*;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ExecutionStatus;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.ServerType;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HtmlRecentPaymentsStrategy extends AbstractSalesCrawlerStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlRecentPaymentsStrategy.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String SIGNATURE_SEPARATOR = ";;;";

    private final CustomerRepository customerRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final LeaderboardSnapshotRepository leaderboardSnapshotRepository;
    private final MonitoredServerRepository monitoredServerRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    private final Map<String, Double> productPriceCache = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> lastCatalogUpdate = new ConcurrentHashMap<>();

    public HtmlRecentPaymentsStrategy(CustomerRepository customerRepository,
                                      SalesTransactionRepository salesTransactionRepository,
                                      LeaderboardSnapshotRepository leaderboardSnapshotRepository,
                                      ExecutionLogRepository executionLogRepository,
                                      MonitoredServerRepository monitoredServerRepository,
                                      ProductRepository productRepository,
                                      ProductCategoryRepository productCategoryRepository
    ) {
        super(executionLogRepository);
        this.customerRepository = customerRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.leaderboardSnapshotRepository = leaderboardSnapshotRepository;
        this.monitoredServerRepository = monitoredServerRepository;
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    private record PendingTransaction(String username, double amount, String signature, List<SalesItem> items) {}

    @Override
    public boolean supports(ServerType type) {
        return type == ServerType.HTML_RECENT_PAYMENTS;
    }

    @Override
    public void execute(MonitoredServer server) {
        LOGGER.info("[{}] Starting HTML crawler...", server.getName());
        LocalDateTime start = LocalDateTime.now();

        int newSales = 0;
        int newCustomers = 0;
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        String errorMessage = null;

        try {
            updatePriceCatalogIfNeeded(server);

            Document document = Jsoup.connect(server.getSalesUrl())
                    .userAgent(USER_AGENT)
                    .timeout(30000)
                    .get();

            Elements recentPayments = document.select(".recent-payments .recent-payments__user");

            List<PendingTransaction> currentTransactions = new ArrayList<>();
            List<String> currentSignatures = new ArrayList<>();

            for(Element element : recentPayments) {
                String tooltipData = element.attr("data-original-title");
                if(tooltipData.isEmpty()) tooltipData = element.attr("title");

                Optional<PendingTransaction> transactionOptional = parseTooltip(tooltipData, server);

                if(transactionOptional.isPresent()) {
                    currentTransactions.add(transactionOptional.get());
                    currentSignatures.add(transactionOptional.get().signature());
                }
            }

            List<String> previousSignatures = new ArrayList<>();
            if(server.getLastCrawledSignatures() != null && !server.getLastCrawledSignatures().isEmpty()) {
                previousSignatures = Arrays.asList(server.getLastCrawledSignatures().split(SIGNATURE_SEPARATOR));
            }

            List<PendingTransaction> newItemsToProcess = new ArrayList<>();

            if(previousSignatures.isEmpty()) {
                newItemsToProcess.addAll(currentTransactions);
            } else {
                int newCount = 0;

                for(int i = 0; i < currentTransactions.size(); i++) {
                    List<String> tail = currentSignatures.subList(i, currentSignatures.size());

                    if(startsWith(previousSignatures, tail)) {
                        newCount = i;
                        break;
                    }

                    newCount = currentTransactions.size();
                }

                for(int i = 0; i < newCount; i++) {
                    newItemsToProcess.add(currentTransactions.get(i));
                }
            }

            LocalDateTime now = LocalDateTime.now();
            for(PendingTransaction transaction : newItemsToProcess) {
                boolean isNewCustomer = processSale(transaction, server, now);
                newSales++;
                if(isNewCustomer) newCustomers++;
            }

            if(!currentSignatures.equals(previousSignatures) && !currentSignatures.isEmpty()) {
                String newStateString = String.join(SIGNATURE_SEPARATOR, currentSignatures);
                server.setLastCrawledSignatures(newStateString);
                monitoredServerRepository.save(server);
            }
        } catch(Exception exception) {
            LOGGER.error("[{}] Critical error crawling HTML", server.getName(), exception);
            status = ExecutionStatus.FAILURE;
            errorMessage = exception.getMessage();
        } finally {
            saveLog(server, start, status, newCustomers, newSales, errorMessage);
        }
    }

    private void updatePriceCatalogIfNeeded(MonitoredServer server) {
        LocalDateTime lastUpdate = lastCatalogUpdate.get(server.getId());

        if(lastUpdate == null || lastUpdate.isBefore(LocalDateTime.now().minusHours(1))) {
            LOGGER.info("[{}] Updating price catalog (Hourly task)...", server.getName());
            lastCatalogUpdate.put(server.getId(), LocalDateTime.now());

            try {
                updatePriceCatalog(server.getSalesUrl(), server);
            } catch(Exception exception) {
                LOGGER.error("[{}] Failed to update price catalog.", server.getName(), exception);
            }
        }
    }

    private void updatePriceCatalog(String salesUrl, MonitoredServer server) throws IOException {
        Document document = Jsoup.connect(salesUrl)
                .userAgent(USER_AGENT)
                .timeout(45000)
                .get();

        Elements categoryLinks = document.select(".navigation__item a[href*='/category/']");

        for(Element link : categoryLinks) {
            String categoryName = link.text().trim();
            String categoryUrl = link.attr("abs:href");
            Long categoryExternalId = extractedIdFromUrl(link.attr("href"));

            ProductCategory category = null;
            if(categoryExternalId != null) {
                category = productCategoryRepository.findByExternalIdAndServer(categoryExternalId, server)
                        .orElse(null);
            }

            if(category == null) {
                category = productCategoryRepository.findByNameAndServer(categoryName, server)
                        .orElseGet(() -> productCategoryRepository.save(ProductCategory.builder()
                                .name(categoryName)
                                .externalId(categoryExternalId)
                                .server(server)
                                .build()));

                if(categoryExternalId != null && category.getExternalId() == null) {
                    category.setExternalId(categoryExternalId);
                    productCategoryRepository.save(category);
                }
            }

            try {
                Thread.sleep(1000);
                Document categoryDocument = Jsoup.connect(categoryUrl).userAgent(USER_AGENT).timeout(45000).ignoreHttpErrors(true).get();
                Elements products = categoryDocument.select(".gridpackage");

                for(Element productElement : products) {
                    String productName = productElement.select(".gridpackage__name").text().trim();
                    Double price = extractPrice(productElement);
                    Long productExternalId = extractProductId(productElement);

                    if(price != null) {
                        productPriceCache.put(productName, price);

                        Product product = null;
                        if(productExternalId != null) {
                            product = productRepository.findByExternalIdAndServer(productExternalId, server)
                                    .orElse(null);
                        }

                        if(product == null) {
                            product = productRepository.findByNameAndServer(productName, server)
                                    .orElse(null);

                            if(product != null && productExternalId != null) {
                                product.setExternalId(productExternalId);
                            }
                        }

                        if(product == null) {
                            product = Product.builder()
                                    .name(productName)
                                    .externalId(productExternalId)
                                    .server(server)
                                    .build();
                        }

                        product.setCurrentPrice(price);
                        product.setCategory(category);
                        product.setName(productName);
                        productRepository.save(product);
                    }
                }
            } catch(Exception exception) {
                LOGGER.error("[{}] Error fetching category {}", server.getName(), categoryUrl, exception);
            }
        }

        LOGGER.debug("[{}] Price catalog updated. Items: {}", server.getName(), productPriceCache.size());
    }

    private Long extractedIdFromUrl(String url) {
        try {
            return Long.parseLong(url.replaceAll("\\D", ""));
        } catch(Exception exception) {
            return null;
        }
    }

    private Double extractPrice(Element productElement) {
        Element priceElement = productElement.select(".gridpackage__price").first();

        if(priceElement != null) {
            priceElement.select("del").remove();
            String priceText = priceElement.text().replaceAll("[^0-9.,]", "").replaceAll(",", ".");

            try {
                if(!priceText.isEmpty()) return Double.parseDouble(priceText);
            } catch(NumberFormatException exception) {
                LOGGER.warn("[{}] Invalid price found: {}", productElement.select(".gridpackage__name").text(), priceText);
            }
        }

        return null;
    }

    private Long extractProductId(Element productElement) {
        Element buyButton = productElement.select("button[atom-package-id]").first();

        if(buyButton != null) {
            try {
                return Long.parseLong(buyButton.attr("atom-package-id"));
            } catch(Exception exception) {
                LOGGER.warn("[{}] Invalid product id found: {}", productElement.select(".gridpackage__name").text(), buyButton.attr("atom-package-id"));
            }
        }

        Element imageLink = productElement.select("a[data-remote^='/package/']").first();

        if(imageLink != null) {
            return extractedIdFromUrl(imageLink.attr("data-remote"));
        }

        return null;
    }

    private Optional<PendingTransaction> parseTooltip(String htmlContent, MonitoredServer server) {
        if(htmlContent == null || htmlContent.isEmpty()) return Optional.empty();

        Document document = Jsoup.parseBodyFragment(htmlContent);
        String username = document.select("b").text().trim();
        String itemsText = document.body().text().replace(username, "").trim();

        double totalAmount = 0.0;
        StringBuilder signatureBuilder = new StringBuilder().append(username).append("|");
        boolean hasValidItems = false;
        List<SalesItem> foundItems = new ArrayList<>();

        String[] items = itemsText.split(",");
        for(String item : items) {
            Pattern pattern = Pattern.compile("(\\d+)\\s*x\\s+(.+)");
            Matcher matcher = pattern.matcher(item.trim());

            if(matcher.find()) {
                hasValidItems = true;
                int quantity = Integer.parseInt(matcher.group(1));
                String productName = matcher.group(2).trim();
                Double price = productPriceCache.getOrDefault(productName, 0.0);

                if(!productPriceCache.containsKey(productName)) {
                    LOGGER.warn("[{}] Missing price for '{}'. Assuming 0.0", server.getName(), productName);
                }

                totalAmount += price * quantity;
                signatureBuilder.append(quantity).append("x").append(productName).append("|");

                Product product = productRepository.findByNameAndServer(productName, server).orElse(null);

                foundItems.add(SalesItem.builder()
                        .product(product)
                        .productName(productName)
                        .quantity(quantity)
                        .unitPrice(price)
                        .build());
            }
        }

        if(hasValidItems) {
            return Optional.of(new PendingTransaction(username, totalAmount, signatureBuilder.toString(), foundItems));
        }

        return Optional.empty();
    }

    private boolean startsWith(List<String> full, List<String> prefix) {
        if(prefix.size() > full.size()) return false;

        for(int i = 0; i < prefix.size(); i++) {
            if(!full.get(i).equals(prefix.get(i))) return false;
        }

        return true;
    }

    private boolean processSale(PendingTransaction transaction, MonitoredServer server, LocalDateTime now) {
        boolean isNewCustomer = false;

        Customer customer = customerRepository.findByUsernameAndServer(transaction.username, server)
                .orElse(null);

        if(customer == null) {
            isNewCustomer = true;

            customer = customerRepository.save(Customer.builder()
                    .username(transaction.username)
                    .server(server)
                    .lastSeen(now)
                    .build());

            LOGGER.info("[{}] New customer found: {}", server.getName(), transaction.username);
        } else {
            customer.setLastSeen(now);
            customerRepository.save(customer);
        }

        SalesTransaction newTransaction = SalesTransaction.builder()
                .customer(customer)
                .server(server)
                .amount(transaction.amount)
                .timestamp(now)
                .build();

        for(SalesItem item : transaction.items()) {
            newTransaction.addItem(item);
        }

        salesTransactionRepository.save(newTransaction);

        Optional<LeaderboardSnapshot> lastSnapshot = leaderboardSnapshotRepository.findTopByCustomerOrderBySnapshotTimeDesc(customer);
        double prevTotal = lastSnapshot.map(LeaderboardSnapshot::getTotalAccumulated).orElse(0.0);

        leaderboardSnapshotRepository.save(LeaderboardSnapshot.builder()
                .customer(customer)
                .server(server)
                .totalAccumulated(prevTotal + transaction.amount)
                .snapshotTime(now)
                .build());

        LOGGER.info("[{}] New sale: {} (+{})", server.getName(), transaction.username, transaction.amount);
        return isNewCustomer;
    }
}