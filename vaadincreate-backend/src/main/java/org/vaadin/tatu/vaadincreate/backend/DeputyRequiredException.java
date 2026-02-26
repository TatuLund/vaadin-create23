package org.vaadin.tatu.vaadincreate.backend;

import org.jspecify.annotations.NullMarked;

/**
 * Thrown by the user service when a user with {@code ADMIN} or {@code USER}
 * role is being deactivated and they still have PENDING purchase approvals
 * assigned to them. The caller must supply a deputy approver and retry.
 */
@NullMarked
public class DeputyRequiredException extends RuntimeException {

    private final int pendingCount;

    /**
     * Constructs a new {@code DeputyRequiredException}.
     *
     * @param pendingCount
     *            the number of PENDING purchases that must be reassigned
     */
    public DeputyRequiredException(int pendingCount) {
        super("Deputy approver required to reassign " + pendingCount
                + " pending purchases");
        this.pendingCount = pendingCount;
    }

    /**
     * Returns the number of PENDING purchases that must be reassigned before
     * the user can be deactivated.
     *
     * @return positive count of pending purchases
     */
    public int getPendingCount() {
        return pendingCount;
    }
}
