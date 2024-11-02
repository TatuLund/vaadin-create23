package org.vaadin.tatu.vaadincreate.stats;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.crud.BooksPresenter.BooksChanged;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;

@SuppressWarnings("serial")
public class StatsPresenter implements EventBusListener, Serializable {

    private StatsView view;
    private transient CompletableFuture<Void> future;
    private static final String PRODUCTS_NOT_NULL = "Products must not be null";

    public StatsPresenter(StatsView view) {
        this.view = view;
        getEventBus().registerEventBusListener(this);
    }

    private CompletableFuture<Collection<Product>> loadProductsAsync() {
        var service = getService();
        return CompletableFuture.supplyAsync(service::getAllProducts,
                getExecutor());
    }

    /**
     * Load the product data in background thread calculate statistics when
     * loading completes. Finally push statitics data to UI.
     */
    public void requestUpdateStats() {
        logger.info("Fetching products for statistics");
        var service = getService();
        future = loadProductsAsync().thenAccept(products -> {
            logger.info("Calculating statistics");

            Map<Availability, Long> availabilityStats = calculateAvailabilityStats(
                    products);

            Map<String, Long[]> categoryStats = calculateCategoryStats(service,
                    products);
            // filter out empty categories
            categoryStats.entrySet()
                    .removeIf(entry -> entry.getValue()[0] == 0);

            Map<String, Long> priceStats = calculatePriceStats(products);
            // filter out empty price brackets
            priceStats.entrySet().removeIf(entry -> entry.getValue() == 0);

            view.updateStatsAsync(availabilityStats, categoryStats, priceStats);
            future = null;
        });
    }

    // Calculate statistics based on product data and return as a map of price
    // brackets and product counts
    private Map<String, Long> calculatePriceStats(
            Collection<Product> products) {
        assert products != null : PRODUCTS_NOT_NULL;
        return getPriceBrackets(products).stream()
                .collect(Collectors.toMap(PriceBracket::toString,
                        priceBracket -> products.stream()
                                .filter(product -> priceBracket
                                        .isInPriceBracket(product.getPrice()))
                                .count()));
    }

    // Calculate statistics based on product data and return as a map of
    // category names and product counts
    private Map<String, Long[]> calculateCategoryStats(
            ProductDataService service, Collection<Product> products) {
        assert products != null : PRODUCTS_NOT_NULL;
        return service.getAllCategories().stream()
                .collect(Collectors.toMap(Category::getName, category -> {
                    long titles = products.stream().filter(
                            product -> product.getCategory().contains(category))
                            .count();
                    long instock = products.stream().filter(
                            product -> product.getCategory().contains(category))
                            .mapToLong(Product::getStockCount).sum();
                    return new Long[] { titles, instock };
                }));
    }

    // Calculate statistics based on product data and return as a map of
    // availability statuses and product counts
    private Map<Availability, Long> calculateAvailabilityStats(
            Collection<Product> products) {
        assert products != null : PRODUCTS_NOT_NULL;
        return Arrays.stream(Availability.values()).map(availability -> Map
                .entry(availability, products.stream().filter(
                        product -> product.getAvailability() == availability)
                        .count()))
                .collect(toEnumMap(Availability.class));
    }

    public void cancelUpdateStats() {
        getEventBus().unregisterEventBusListener(this);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            future = null;
            logger.info("Fetching stats cancelled: {}", cancelled);
        }
    }

    private List<PriceBracket> getPriceBrackets(Collection<Product> products) {
        assert products != null : PRODUCTS_NOT_NULL;

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

    private ProductDataService getService() {
        return VaadinCreateUI.get().getProductService();
    }

    private ExecutorService getExecutor() {
        return VaadinCreateUI.get().getExecutor();
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

    @Override
    public void eventFired(Object event) {
        // Update statistics when new product is added
        if (event instanceof BooksChanged) {
            logger.info("Book saved or deleted, refreshing statistics.");
            view.setLoading();
            requestUpdateStats();
        }
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

        @Override
        public String toString() {
            return (max - 10) + " - " + max + " €";
        }
    }

    // Collectors.toMap does not support EnumMap, so we need to implement our
    // own collector
    private static <K extends Enum<K>, V> Collector<Map.Entry<K, V>, ?, EnumMap<K, V>> toEnumMap(
            Class<K> keyType) {
        return Collector.of(() -> new EnumMap<>(keyType),
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                (map1, map2) -> {
                    map1.putAll(map2);
                    return map1;
                });
    }

    private static Logger logger = LoggerFactory
            .getLogger(StatsPresenter.class);

}
