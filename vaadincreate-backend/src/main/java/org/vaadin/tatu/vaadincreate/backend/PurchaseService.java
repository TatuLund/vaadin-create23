package org.vaadin.tatu.vaadincreate.backend;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.service.PurchaseServiceImpl;

/**
 * Service interface for managing purchases. Provides operations for creating
 * and querying purchase requests.
 */
@NullMarked
public interface PurchaseService {

    /**
     * Creates a new pending purchase from a cart. This method: - Creates a
     * Purchase with status PENDING - Snapshots the delivery address - Creates
     * PurchaseLines with current product prices - Does NOT modify product stock
     *
     * @param cart
     *            the cart containing products and quantities
     * @param address
     *            the delivery address for this purchase
     * @param requester
     *            the user making the purchase request
     * @param defaultApproverOrNull
     *            the approver to assign, or null to use default
     * @return the created purchase
     */
    Purchase createPendingPurchase(Cart cart, Address address, User requester,
            @Nullable User defaultApproverOrNull);

    /**
     * Finds purchases created by a specific user.
     *
     * @param requester
     *            the user who created the purchases
     * @param offset
     *            the starting offset for pagination
     * @param limit
     *            the maximum number of results
     * @return list of purchases
     */
    List<@NonNull Purchase> findMyPurchases(User requester, int offset,
            int limit);

    /**
     * Counts purchases created by a specific user.
     *
     * @param requester
     *            the user who created the purchases
     * @return the count of purchases
     */
    long countMyPurchases(User requester);

    /**
     * Finds all purchases with pagination.
     *
     * @param offset
     *            the starting offset for pagination
     * @param limit
     *            the maximum number of results
     * @return list of all purchases
     */
    List<@NonNull Purchase> findAll(int offset, int limit);

    /**
     * Counts all purchases.
     *
     * @return the total count of purchases
     */
    long countAll();

    /**
     * Finds pending purchases assigned to a specific approver.
     *
     * @param approver
     *            the approver user
     * @param offset
     *            the starting offset for pagination
     * @param limit
     *            the maximum number of results
     * @return list of pending purchases
     */
    List<@NonNull Purchase> findPendingForApprover(User approver, int offset,
            int limit);

    /**
     * Counts pending purchases assigned to a specific approver.
     *
     * @param approver
     *            the approver user
     * @return the count of pending purchases
     */
    long countPendingForApprover(User approver);

    /**
     * Finds purchases for a user that have been decided (COMPLETED, REJECTED,
     * or CANCELLED) since a given timestamp.
     *
     * @param requester
     *            the user who created the purchases
     * @param since
     *            the timestamp to filter from
     * @return list of decided purchases since the given time
     */
    List<@NonNull Purchase> findRecentlyDecidedPurchases(User requester,
            java.time.Instant since);

    /**
     * Gets the singleton instance of the PurchaseService.
     *
     * @return the PurchaseService instance
     */
    static PurchaseService get() {
        return PurchaseServiceImpl.getInstance();
    }
}
