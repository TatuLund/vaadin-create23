package org.vaadin.tatu.vaadincreate.purchases;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.PurchaseSavedEvent;
import org.vaadin.tatu.vaadincreate.backend.events.PurchaseStatusChangedEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;

/**
 * Presenter for fetching purchase history data. Subscribes to
 * {@link PurchaseStatusChangedEvent} and {@link PurchaseSavedEvent} to provide
 * live grid updates.
 */
@NullMarked
@SuppressWarnings("serial")
public class PurchaseHistoryPresenter
        implements Serializable, EventBusListener {

    private static final Logger logger = LoggerFactory
            .getLogger(PurchaseHistoryPresenter.class);

    @Nullable
    private transient PurchaseService purchaseService;

    @Nullable
    private PurchaseHistoryGrid grid;

    @Nullable
    private User currentUser;

    /**
     * Fetches purchases for the given mode and user.
     *
     * @param mode
     *            the history mode to use
     * @param offset
     *            the starting offset for pagination
     * @param limit
     *            the maximum number of results
     * @param currentUser
     *            the current user driving the query
     * @return list of purchases
     */
    public List<@NonNull Purchase> fetchPurchases(PurchaseHistoryMode mode,
            int offset, int limit, User currentUser) {
        Objects.requireNonNull(mode, "Mode must not be null");
        Objects.requireNonNull(currentUser, "Current user must not be null");
        return getPurchaseService().fetchPurchases(mode, offset, limit,
                currentUser);
    }

    /**
     * Counts purchases for the given mode and user.
     *
     * @param mode
     *            the history mode to use
     * @param currentUser
     *            the current user driving the query
     * @return the count of purchases
     */
    public long countPurchases(PurchaseHistoryMode mode, User currentUser) {
        Objects.requireNonNull(mode, "Mode must not be null");
        Objects.requireNonNull(currentUser, "Current user must not be null");
        return getPurchaseService().countPurchases(mode, currentUser);
    }

    /**
     * Registers this presenter with the EventBus and stores the grid and user
     * references for live update callbacks. Called from
     * {@link PurchaseHistoryGrid} when it is constructed.
     *
     * @param grid
     *            the grid to refresh on events
     * @param currentUser
     *            the currently logged-in user
     */
    public void register(PurchaseHistoryGrid grid, User currentUser) {
        this.grid = Objects.requireNonNull(grid, "Grid must not be null");
        this.currentUser = Objects.requireNonNull(currentUser,
                "Current user must not be null");
        getEventBus().registerEventBusListener(this);
    }

    /**
     * Unregisters this presenter from the EventBus. Called from
     * {@link PurchaseHistoryGrid} when it is detached.
     */
    public void unregister() {
        getEventBus().unregisterEventBusListener(this);
    }

    @Override
    public void eventFired(AbstractEvent event) {
        switch (event) {
        case PurchaseStatusChangedEvent(Integer purchaseId) ->
            handleStatusChanged(purchaseId);
        case PurchaseSavedEvent(Integer purchaseId) ->
            handlePurchaseSaved(purchaseId);
        default -> {
            // No action for other events
        }
        }
    }

    private void handleStatusChanged(Integer purchaseId) {
        var capturedGrid = grid;
        var capturedUser = currentUser;
        if (capturedGrid == null || capturedUser == null) {
            return;
        }
        var purchase = getPurchaseService().fetchPurchaseById(purchaseId);
        if (purchase == null) {
            return;
        }
        if (!capturedUser.equals(purchase.getRequester())) {
            return;
        }
        logger.info(
                "PurchaseStatusChangedEvent received for purchase {}, refreshing grid",
                purchaseId);
        capturedGrid.showStatusNotificationAsync(purchase);
        capturedGrid.refreshAsync();
    }

    private void handlePurchaseSaved(Integer purchaseId) {
        var capturedGrid = grid;
        var capturedUser = currentUser;
        if (capturedGrid == null || capturedUser == null) {
            return;
        }
        var purchase = getPurchaseService().fetchPurchaseById(purchaseId);
        if (purchase == null) {
            return;
        }
        boolean isAdmin = capturedUser.getRole() == Role.ADMIN;
        boolean isApprover = capturedUser.equals(purchase.getApprover());
        boolean isRequester = capturedUser.equals(purchase.getRequester());
        if (!isAdmin && !isApprover && !isRequester) {
            return;
        }
        logger.info(
                "PurchaseSavedEvent received for purchase {}, refreshing grid",
                purchaseId);
        capturedGrid.refreshAsync();
    }

    private PurchaseService getPurchaseService() {
        if (purchaseService == null) {
            purchaseService = PurchaseService.get();
        }
        return purchaseService;
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }
}
