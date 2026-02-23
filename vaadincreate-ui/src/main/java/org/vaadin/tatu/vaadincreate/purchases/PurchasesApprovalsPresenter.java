package org.vaadin.tatu.vaadincreate.purchases;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.OptimisticLockException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.events.PurchaseStatusChangedEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.util.Utils;

/**
 * Presenter for the approvals tab. Handles approve and reject operations,
 * including a single retry on optimistic lock conflicts.
 */
@NullMarked
@SuppressWarnings("serial")
public class PurchasesApprovalsPresenter implements Serializable {

    @Nullable
    private transient PurchaseService purchaseService;

    /**
     * Approves the given purchase, retrying once on optimistic lock conflict.
     *
     * @param purchaseId
     *            the purchase to approve
     * @param currentUser
     *            the approver
     * @param decisionCommentOrNull
     *            optional comment
     * @return result of the approve operation
     * @throws OptimisticLockException
     *             if two consecutive attempts both encounter a conflict
     */
    public ApproveResult approve(Integer purchaseId, User currentUser,
            @Nullable String decisionCommentOrNull) {
        Objects.requireNonNull(purchaseId, "Purchase ID must not be null");
        Objects.requireNonNull(currentUser, "Current user must not be null");
        try {
            var purchase = getPurchaseService().approve(purchaseId, currentUser,
                    decisionCommentOrNull);
            postStatusChangedEvent(purchaseId);
            if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
                return ApproveResult.cancelled(purchase);
            }
            return ApproveResult.completed(purchase);
        } catch (Exception e) {
            if (Utils.throwableHasCause(e, OptimisticLockException.class)) {
                logger.warn(
                        "OptimisticLockException on first approve attempt for purchase ({}), retrying",
                        purchaseId);
                // Retry once.
                var purchase = getPurchaseService().approve(purchaseId,
                        currentUser, decisionCommentOrNull);
                postStatusChangedEvent(purchaseId);
                if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
                    return ApproveResult.cancelled(purchase);
                }
                return ApproveResult.completed(purchase);
            }
            throw e;
        }
    }

    /**
     * Rejects the given purchase.
     *
     * @param purchaseId
     *            the purchase to reject
     * @param currentUser
     *            the user performing the rejection
     * @param reason
     *            non-empty reason for rejection
     * @return the updated purchase
     */
    public Purchase reject(Integer purchaseId, User currentUser,
            String reason) {
        Objects.requireNonNull(purchaseId, "Purchase ID must not be null");
        Objects.requireNonNull(currentUser, "Current user must not be null");
        Objects.requireNonNull(reason, "Reason must not be null");
        var purchase = getPurchaseService().reject(purchaseId, currentUser,
                reason);
        postStatusChangedEvent(purchaseId);
        return purchase;
    }

    private void postStatusChangedEvent(Integer purchaseId) {
        getEventBus().post(new PurchaseStatusChangedEvent(purchaseId));
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

    /**
     * Result of an approve operation, carrying outcome and optional details.
     */
    public sealed interface ApproveResult
            permits ApproveResult.Completed, ApproveResult.Cancelled {

        /** The purchase after the approve attempt. */
        Purchase purchase();

        /** Returns {@code true} if the purchase was completed successfully. */
        default boolean isCompleted() {
            return this instanceof Completed;
        }

        /** Returns {@code true} if the purchase was cancelled due to stock. */
        default boolean isCancelled() {
            return this instanceof Cancelled;
        }

        /**
         * Creates a {@link Completed} result.
         *
         * @param purchase
         *            the completed purchase
         * @return a Completed result
         */
        static ApproveResult completed(Purchase purchase) {
            return new Completed(purchase);
        }

        /**
         * Creates a {@link Cancelled} result.
         *
         * @param purchase
         *            the cancelled purchase
         * @return a Cancelled result
         */
        static ApproveResult cancelled(Purchase purchase) {
            return new Cancelled(purchase);
        }

        /**
         * Purchase approved and stock decremented.
         *
         * @param purchase
         *            the completed purchase
         */
        record Completed(Purchase purchase) implements ApproveResult {
        }

        /**
         * Purchase cancelled due to insufficient stock.
         *
         * @param purchase
         *            the cancelled purchase
         */
        record Cancelled(Purchase purchase) implements ApproveResult {
        }
    }

    @SuppressWarnings("null")
    private static final Logger logger = LoggerFactory
            .getLogger(PurchasesApprovalsPresenter.class);
}
