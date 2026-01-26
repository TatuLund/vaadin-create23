package org.vaadin.tatu.vaadincreate.stats;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.BooksChangedEvent;
import org.vaadin.tatu.vaadincreate.backend.events.CategoriesUpdatedEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.stats.StatsUtils.CategoryStats;

@NullMarked
@SuppressWarnings("serial")
public class StatsPresenter implements EventBusListener, Serializable {

    private static final Logger logger = LoggerFactory
            .getLogger(StatsPresenter.class);

    private StatsView view;
    @Nullable
    private transient CompletableFuture<Void> future;
    private transient ProductDataService service = VaadinCreateUI.get()
            .getProductService();
    private transient ExecutorService executor = VaadinCreateUI.get()
            .getExecutor();

    public StatsPresenter(StatsView view) {
        this.view = view;
        getEventBus().registerEventBusListener(this);
    }

    record ProductData(Collection<Product> products,
            Collection<Category> categories) {
    }

    private CompletableFuture<ProductData> loadProductsAsync() {
        var productService = getService();
        return CompletableFuture
                .supplyAsync(
                        () -> new ProductData(productService.getAllProducts(),
                                productService.getAllCategories()),
                        getExecutor());
    }

    /**
     * Load the product data in background thread calculate statistics when
     * loading completes. Finally push statistics data to UI.
     */
    public void requestUpdateStats() {
        logger.info("Fetching products for statistics");
        future = loadProductsAsync().thenAccept(this::calculateStatistics)
                .whenComplete((result, throwable) -> future = null);
    }

    /**
     * A record to hold product statistics.
     *
     * @param availabilityStats
     *           A map containing availability statistics.
     * @param categoryStats
     *          A map containing category statistics.
     * @param priceStats
     *          A map containing price statistics.
     */
    public record ProductStatistics(Map<Availability, Long> availabilityStats,
            Map<String, CategoryStats> categoryStats,
            Map<String, Long> priceStats) {
    }

    private void calculateStatistics(ProductData productData) {
        var start = System.currentTimeMillis();
        logger.info("Calculating statistics");

        var stats = new ProductStatistics(
                StatsUtils.calculateAvailabilityStats(productData.products()),
                StatsUtils.calculateCategoryStats(productData.categories(),
                        productData.products()),
                StatsUtils.calculatePriceStats(productData.products()));

        view.updateStatsAsync(stats);
        logger.info("Statistics updated in {}ms",
                System.currentTimeMillis() - start);
    }

    public void cancelUpdateStats() {
        getEventBus().unregisterEventBusListener(this);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            future = null;
            logger.info("Fetching stats cancelled: {}", cancelled);
        }
    }

    private ProductDataService getService() {
        if (service == null) {
            service = VaadinCreateUI.get().getProductService();
        }
        return service;
    }

    private ExecutorService getExecutor() {
        if (executor == null) {
            executor = VaadinCreateUI.get().getExecutor();
        }
        return executor;
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

    @Override
    public void eventFired(AbstractEvent event) {
        // Update statistics when any relevant event occurs
        if (event instanceof BooksChangedEvent
                || event instanceof CategoriesUpdatedEvent) {
            logger.info(
                    "Book or Category saved or deleted, refreshing statistics.");
            view.setLoadingAsync();
            requestUpdateStats();
        }
    }

}
