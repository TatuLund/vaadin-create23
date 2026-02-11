package org.vaadin.tatu.vaadincreate.backend.data;

/**
 * Enum representing the status of a purchase request.
 * 
 * <p>The lifecycle of a purchase typically progresses as follows:
 * <ul>
 * <li>{@link #PENDING} - Initial state when a purchase request is created</li>
 * <li>{@link #COMPLETED} - Purchase approved, stock decremented</li>
 * <li>{@link #REJECTED} - Purchase rejected by supervisor</li>
 * <li>{@link #CANCELLED} - Purchase cancelled due to insufficient inventory</li>
 * </ul>
 */
public enum PurchaseStatus {
    /**
     * Purchase is waiting for supervisor approval or rejection.
     */
    PENDING,
    
    /**
     * Purchase has been approved and stock has been decremented.
     * This is the final successful state.
     */
    COMPLETED,
    
    /**
     * Purchase was explicitly rejected by the supervisor.
     * No stock changes occurred.
     */
    REJECTED,
    
    /**
     * Purchase was cancelled because items were missing in inventory
     * at the time of approval.
     */
    CANCELLED
}
