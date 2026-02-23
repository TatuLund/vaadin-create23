package org.vaadin.tatu.vaadincreate.purchases;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService.MonthlyPurchaseStat;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService.ProductPurchaseStat;

/**
 * Presenter responsible for loading purchase statistics asynchronously and
 * pushing the result to {@link PurchasesStatsView}.
 *
 * <p>
 * Follows the same pattern as {@code StatsPresenter}: data is loaded in a
 * background thread via a {@link CompletableFuture} using the shared
 * {@link ExecutorService}, and the UI is updated through
 * {@code Utils.access()}.
 */
@NullMarked
@SuppressWarnings("serial")
public class PurchasesStatsPresenter implements Serializable {

    private static final Logger logger = LoggerFactory
            .getLogger(PurchasesStatsPresenter.class);

    /** Number of products shown in the top / least charts. */
    static final int PRODUCT_LIMIT = 10;

    /** Number of calendar months shown in the monthly line chart. */
    static final int MONTHLY_RANGE = 12;

    private final PurchasesStatsView view;

    @Nullable
    private transient CompletableFuture<Void> future;

    @Nullable
    private transient PurchaseService service;

    @Nullable
    private transient ExecutorService executor;

    /**
     * Record carrying all three stat datasets required by the Stats tab.
     *
     * @param topProducts
     *            top {@value #PRODUCT_LIMIT} products by purchased quantity
     * @param leastProducts
     *            least {@value #PRODUCT_LIMIT} products by purchased quantity
     * @param monthlyTotals
     *            monthly totals for the last {@value #MONTHLY_RANGE} months
     */
    public record PurchaseStatistics(List<ProductPurchaseStat> topProducts,
            List<ProductPurchaseStat> leastProducts,
            List<MonthlyPurchaseStat> monthlyTotals) {
    }

    /**
     * Creates a new presenter bound to the given view.
     *
     * @param view
     *            the Stats tab to push updates to, must not be null
     */
    public PurchasesStatsPresenter(PurchasesStatsView view) {
        this.view = view;
    }

    /**
     * Triggers an asynchronous load of purchase statistics and pushes the
     * result to the view once complete.
     */
    public void requestUpdateStats() {
        logger.info("Fetching purchase statistics");
        future = CompletableFuture
                .supplyAsync(this::loadStats, getExecutor())
                .thenAccept(view::updateStatsAsync)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to load purchase statistics",
                                throwable);
                    }
                    future = null;
                });
    }

    /**
     * Cancels any in-progress statistics load.
     */
    public void cancelUpdateStats() {
        if (future != null) {
            boolean cancelled = future.cancel(true);
            future = null;
            logger.info("Fetching purchase stats cancelled: {}", cancelled);
        }
    }

    private PurchaseStatistics loadStats() {
        var svc = getService();
        return new PurchaseStatistics(
                svc.getTopProductsByQuantity(PRODUCT_LIMIT),
                svc.getLeastProductsByQuantity(PRODUCT_LIMIT),
                svc.getMonthlyTotals(MONTHLY_RANGE));
    }

    private PurchaseService getService() {
        if (service == null) {
            service = PurchaseService.get();
        }
        return service;
    }

    private ExecutorService getExecutor() {
        if (executor == null) {
            executor = VaadinCreateUI.get().getExecutor();
        }
        return executor;
    }
}
