package org.vaadin.tatu.vaadincreate.backend;

import java.math.BigDecimal;
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
 * and querying purchase requests, and for computing purchase statistics.
 */
@NullMarked
public interface PurchaseService {

    /**
     * Aggregated purchase quantity for a single product (COMPLETED purchases
     * only).
     *
     * @param productId
     *            the product id
     * @param productName
     *            the product name
     * @param quantity
     *            total purchased quantity across all COMPLETED purchase lines
     */
    record ProductPurchaseStat(Integer productId, String productName,
            long quantity) {
    }

    /**
     * Aggregated purchase amount for a single calendar month.
     *
     * @param yearMonth
     *            the month in {@code YYYY-MM} format
     * @param totalAmount
     *            sum of {@code unitPrice * quantity} for all COMPLETED purchase
     *            lines decided in that month
     */
    record MonthlyPurchaseStat(String yearMonth, BigDecimal totalAmount) {
    }

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
     * Fetches purchases for a given history mode with pagination.
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
    List<@NonNull Purchase> fetchPurchases(PurchaseHistoryMode mode, int offset,
            int limit, User currentUser);

    /**
     * Counts purchases for a given history mode.
     *
     * @param mode
     *            the history mode to use
     * @param currentUser
     *            the current user driving the query
     * @return the count of purchases
     */
    long countPurchases(PurchaseHistoryMode mode, User currentUser);

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
     * Approves a pending purchase and decrements product stock. If any product
     * has insufficient stock, the purchase is set to CANCELLED with details.
     *
     * @param purchaseId
     *            the ID of the purchase to approve
     * @param currentUser
     *            the user performing the approval (must be the assigned
     *            approver)
     * @param decisionCommentOrNull
     *            optional comment to attach to the decision
     * @return the updated purchase (status COMPLETED or CANCELLED)
     * @throws IllegalArgumentException
     *             if the purchase is not PENDING or currentUser is not the
     *             approver
     */
    Purchase approve(Integer purchaseId, User currentUser,
            @Nullable String decisionCommentOrNull);

    /**
     * Rejects a pending purchase with a required reason.
     *
     * @param purchaseId
     *            the ID of the purchase to reject
     * @param currentUser
     *            the user performing the rejection
     * @param reason
     *            the required reason for rejection
     * @return the updated purchase (status REJECTED)
     * @throws IllegalArgumentException
     *             if the purchase is not PENDING
     */
    Purchase reject(Integer purchaseId, User currentUser, String reason);

    /**
     * Returns the top products by purchased quantity from COMPLETED purchases,
     * ordered descending.
     *
     * @param limit
     *            maximum number of products to return
     * @return list of at most {@code limit} products, most purchased first
     */
    List<@NonNull ProductPurchaseStat> getTopProductsByQuantity(int limit);

    /**
     * Returns the least purchased products by quantity from COMPLETED
     * purchases, ordered ascending. Products with zero purchased quantity are
     * excluded.
     *
     * @param limit
     *            maximum number of products to return
     * @return list of at most {@code limit} products, least purchased first
     */
    List<@NonNull ProductPurchaseStat> getLeastProductsByQuantity(int limit);

    /**
     * Returns monthly purchase totals (amount) for COMPLETED purchases over the
     * last {@code months} calendar months (including the current month). Months
     * with no purchases are included with a total of {@code 0}.
     *
     * @param months
     *            number of calendar months to include, must be positive
     * @return list of monthly totals ordered by month ascending
     */
    List<@NonNull MonthlyPurchaseStat> getMonthlyTotals(int months);

    /**
     * Gets the singleton instance of the PurchaseService.
     *
     * @return the PurchaseService instance
     */
    static PurchaseService get() {
        return PurchaseServiceImpl.getInstance();
    }
}
