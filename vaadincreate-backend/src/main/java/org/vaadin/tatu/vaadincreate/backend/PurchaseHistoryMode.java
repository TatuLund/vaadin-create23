package org.vaadin.tatu.vaadincreate.backend;

import org.jspecify.annotations.NullMarked;

/**
 * Modes for querying purchase history.
 */
@NullMarked
public enum PurchaseHistoryMode {
    /**
     * Purchases created by the current user.
     */
    MY_PURCHASES,
    /**
     * All purchases (admin/user history).
     */
    ALL,
    /**
     * Purchases pending approval for the current user.
     */
    PENDING_APPROVALS
}
