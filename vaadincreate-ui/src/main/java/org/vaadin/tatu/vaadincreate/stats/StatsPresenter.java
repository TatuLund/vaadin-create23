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
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.BooksChangedEvent;
import org.vaadin.tatu.vaadincreate.backend.events.CategoriesUpdatedEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;

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

    private CompletableFuture<Collection<Product>> loadProductsAsync() {
        var productService = getService();
        return CompletableFuture.supplyAsync(productService::getAllProducts,
                getExecutor());
    }

    /**
     * Load the product data in background thread calculate statistics when
     * loading completes. Finally push statistics data to UI.
     */
    public void requestUpdateStats() {
        logger.info("Fetching products for statistics");
        future = loadProductsAsync().thenAccept(products -> {
            var start = System.currentTimeMillis();
            logger.info("Calculating statistics");

            Map<Availability, Long> availabilityStats = StatsUtils
                    .calculateAvailabilityStats(products);

            var categories = service.getAllCategories();
            Map<String, Long[]> categoryStats = StatsUtils
                    .calculateCategoryStats(categories, products);

            Map<String, Long> priceStats = StatsUtils
                    .calculatePriceStats(products);

            view.updateStatsAsync(availabilityStats, categoryStats, priceStats);
            logger.info("Statistics updated in {}ms",
                    System.currentTimeMillis() - start);
        }).whenComplete((result, throwable) -> future = null);
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
