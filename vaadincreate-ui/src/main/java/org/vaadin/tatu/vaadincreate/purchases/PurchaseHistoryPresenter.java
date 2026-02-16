package org.vaadin.tatu.vaadincreate.purchases;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.User;

/**
 * Presenter for fetching purchase history data.
 */
@NullMarked
@SuppressWarnings("serial")
public class PurchaseHistoryPresenter implements Serializable {

    private transient PurchaseService purchaseService;

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

    private PurchaseService getPurchaseService() {
        if (purchaseService == null) {
            purchaseService = PurchaseService.get();
        }
        return purchaseService;
    }
}
