package org.vaadin.tatu.vaadincreate.stats;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;

@SuppressWarnings("serial")
public class StatsPresenter implements Serializable {

    private StatsView view;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private transient CompletableFuture<Void> future;
    private ProductDataService service = VaadinCreateUI.get()
            .getProductService();

    public StatsPresenter(StatsView view) {
        this.view = view;
    }

    private CompletableFuture<Collection<Product>> loadProductsAsync() {
        return CompletableFuture.supplyAsync(() -> service.getAllProducts(),
                executor);
    }

    /**
     * Load the product data in background thread calculate statistics when
     * loading completes. Finally push statitics data to UI.
     */
    public void requestUpdateStats() {
        logger.info("Fetching products for statistics");
        future = loadProductsAsync().thenAccept(products -> {
            logger.info("Calculating statistics");
            Map<Availability, Long> availabilityStats = new HashMap<>();
            for (Availability availability : Availability.values()) {
                var count = products.stream().filter(
                        product -> product.getAvailability() == availability)
                        .count();
                availabilityStats.put(availability, count);
            }

            Map<String, Long[]> categoryStats = new HashMap<>();
            var categories = service.getAllCategories();
            for (Category category : categories) {
                var titles = products.stream().filter(
                        product -> product.getCategory().contains(category))
                        .count();
                var instock = products.stream().filter(
                        product -> product.getCategory().contains(category))
                        .mapToLong(prod -> prod.getStockCount()).sum();
                Long[] counts = {titles, instock};
                categoryStats.put(category.getName(), counts);
            }

            Map<String, Long> priceStats = new HashMap<>();
            for (PriceBracket priceBracket : getPriceBrackets(products)) {
                var count = products.stream().filter(product -> priceBracket
                        .isInPriceBracket(product.getPrice())).count();
                priceStats.put(priceBracket.toString(), count);
            }

            view.updateStatsAsync(availabilityStats, categoryStats, priceStats);
            future = null;
        });
    }

    public void cancelUpdateStats() {
        if (future != null) {
            boolean cancelled = future.cancel(true);
            future = null;
            logger.info("Fetching stats cancelled: " + cancelled);
        }
    }

    private List<PriceBracket> getPriceBrackets(Collection<Product> products) {
        var brackets = new ArrayList<PriceBracket>();
        var max = products.stream()
                .max((p1, p2) -> p1.getPrice().compareTo(p2.getPrice())).get()
                .getPrice();
        var numBrackets = (max.intValue() / 10) + 1;
        for (int i = 10; i <= (numBrackets * 10); i += 10) {
            brackets.add(new PriceBracket(i));
        }
        return brackets;
    }

    private static class PriceBracket {
        private int max;

        public PriceBracket(int max) {
            this.max = max;
        }

        public boolean isInPriceBracket(BigDecimal price) {
            return price.doubleValue() < max
                    && price.doubleValue() >= (max - 10);
        }

        public String toString() {
            return (max - 10) + " - " + max + " €";
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
