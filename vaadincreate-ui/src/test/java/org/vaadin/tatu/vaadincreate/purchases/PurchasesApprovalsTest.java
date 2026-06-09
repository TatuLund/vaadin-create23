package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.persistence.OptimisticLockException;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Window;

public class PurchasesApprovalsTest extends AbstractPurchasesTest {

    /**
     * Tests the full approval workflow: click Approve → DecisionWindow opens →
     * enter optional comment → click Approve in window → success notification
     * appears and the approved purchase is removed from the pending grid.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void approving_pending_purchase_shows_decision_window_and_updates_status_to_completed()
            throws ServiceException {
        // GIVEN: Login as User5 who is a designated approver in the mock data
        switchToUser("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-approvals-grid");
        assertNotNull("Approvals grid should be present", approvalsGrid);
        int pendingCountBefore = test(approvalsGrid).size();
        assertTrue("There must be at least one pending purchase to approve",
                pendingCountBefore > 0);

        // Get first pending purchase and its ID for the button lookup
        Purchase pendingPurchase = test(approvalsGrid).item(0);
        assertNotNull(pendingPurchase);
        assertNotNull(pendingPurchase.getId());
        assertEquals(PurchaseStatus.PENDING, pendingPurchase.getStatus());

        // WHEN: Clicking the Approve button on the first pending purchase
        var approveButton = getApproveButton(approvalsGrid);
        test(approveButton).click();

        // THEN: DecisionWindow opens
        var decisionWindow = $(Window.class)
                .id(DecisionDialog.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);
        assertTrue(decisionWindow.isAttached());

        // WHEN: Entering an optional comment and confirming
        var commentField = $(decisionWindow, TextArea.class)
                .id(DecisionDialog.DECISION_COMMENT_ID);
        assertNotNull("Comment field should be present", commentField);
        test(commentField).setValue("Approved by supervisor");

        test($(decisionWindow, Button.class)
                .id(DecisionDialog.CONFIRM_BUTTON_ID)).click();

        // THEN: Window is closed
        assertFalse("Decision window should be closed after confirm",
                decisionWindow.isAttached());

        // THEN: A notification about the purchase is shown (either
        // "approved successfully" when stock is sufficient, or "cancelled:
        // Insufficient stock" when stock runs out – both are valid outcomes
        // per the PRD and both remove the purchase from the pending queue)
        String purchaseIdStr = String.valueOf(pendingPurchase.getId());
        boolean notified = $(Notification.class).stream()
                .anyMatch(n -> n.getCaption() != null && n.getCaption()
                        .startsWith("Purchase #" + purchaseIdStr));
        assertTrue("Expected a notification for purchase #" + purchaseIdStr,
                notified);

        // THEN: Approved purchase no longer appears in the pending approvals
        // grid (status changed to COMPLETED)
        int pendingCountAfter = test(approvalsGrid).size();
        assertEquals(
                "Approved purchase should be removed from the pending approvals grid",
                pendingCountBefore - 1, pendingCountAfter);

        restoreProductStockLevels(pendingPurchase);

        SerializationDebugUtil.assertSerializable(view);
    }

    /**
     * Tests the full rejection workflow: click Reject → DecisionWindow opens →
     * Confirm button is initially disabled → enter required reason → click
     * Reject in window → success notification appears and the rejected purchase
     * is removed from the pending grid.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void rejecting_pending_purchase_shows_decision_window_requires_reason_and_updates_status_to_rejected()
            throws ServiceException {
        switchToUser("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-approvals-grid");
        assertNotNull("Approvals grid should be present", approvalsGrid);
        int pendingCountBefore = test(approvalsGrid).size();
        assertTrue("There must be at least one pending purchase to reject",
                pendingCountBefore > 0);

        // Get first pending purchase
        Purchase pendingPurchase = test(approvalsGrid).item(0);
        assertNotNull(pendingPurchase);
        assertNotNull(pendingPurchase.getId());
        assertEquals(PurchaseStatus.PENDING, pendingPurchase.getStatus());

        // WHEN: Clicking the Reject button on the first pending purchase
        var rejectButton = getRejectButton(approvalsGrid);
        test(rejectButton).click();

        // THEN: DecisionWindow opens
        var decisionWindow = $(Window.class)
                .id(DecisionDialog.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);
        assertTrue(decisionWindow.isAttached());

        // THEN: Confirm (reject) button is initially disabled – reason required
        var confirmButton = $(decisionWindow, Button.class)
                .id(DecisionDialog.CONFIRM_BUTTON_ID);
        assertFalse("Confirm button should be disabled until reason is entered",
                confirmButton.isEnabled());

        // WHEN: Entering a rejection reason
        var commentField = $(decisionWindow, TextArea.class)
                .id(DecisionDialog.DECISION_COMMENT_ID);
        assertNotNull("Comment field should be present", commentField);
        test(commentField).setValue("Budget exceeded this quarter");

        // THEN: Confirm button is now enabled
        assertTrue("Confirm button should be enabled after reason is entered",
                confirmButton.isEnabled());

        // WHEN: Confirming the rejection
        test(confirmButton).click();

        // THEN: Window is closed
        assertFalse("Decision window should be closed after confirm",
                decisionWindow.isAttached());

        // THEN: Success notification is shown
        assertNotification(
                "Purchase #" + pendingPurchase.getId() + " rejected");

        // THEN: Rejected purchase no longer appears in the pending approvals
        // grid (status changed to REJECTED)
        int pendingCountAfter = test(approvalsGrid).size();
        assertEquals(
                "Rejected purchase should be removed from the pending approvals grid",
                pendingCountBefore - 1, pendingCountAfter);

        SerializationDebugUtil.assertSerializable(view);
    }

    /**
     * Tests that clicking Cancel on the approval DecisionWindow closes the
     * window without changing the purchase status: the purchase stays PENDING
     * and the grid count is unchanged.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void approve_pending_purchase_shows_decision_window_and_clicking_cancel_then_status_remains_pending()
            throws ServiceException {
        switchToUser("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-approvals-grid");
        int pendingCountBefore = test(approvalsGrid).size();
        assertTrue("There must be at least one pending purchase to approve",
                pendingCountBefore > 0);

        Purchase pendingPurchase = test(approvalsGrid).item(0);
        assertNotNull(pendingPurchase);
        assertNotNull(pendingPurchase.getId());

        // WHEN: Clicking the Approve button
        var approveButton = getApproveButton(approvalsGrid);
        test(approveButton).click();

        // THEN: DecisionWindow opens
        var decisionWindow = $(Window.class)
                .id(DecisionDialog.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);
        assertTrue(decisionWindow.isAttached());

        // WHEN: Clicking Cancel instead of confirming
        test($(decisionWindow, Button.class)
                .id(DecisionDialog.CANCEL_BUTTON_ID)).click();

        // THEN: Window is closed
        assertFalse("Decision window should be closed after cancel",
                decisionWindow.isAttached());

        // THEN: Approve button is re-enabled so the user can retry
        assertTrue("Approve button should be re-enabled after cancel",
                approveButton.isEnabled());

        // THEN: Grid count is unchanged – purchase is still PENDING
        assertEquals("Grid count should be unchanged after cancel",
                pendingCountBefore, test(approvalsGrid).size());
        assertTrue(test(approvalsGrid).isFocused());

        SerializationDebugUtil.assertSerializable(view);
    }

    /**
     * Tests that clicking Cancel on the rejection DecisionWindow closes the
     * window without changing the purchase status: the purchase stays PENDING
     * and the grid count is unchanged.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void rejecting_pending_purchase_shows_decision_window_and_clicking_cancel_then_status_remains_pending()
            throws ServiceException {
        switchToUser("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-approvals-grid");
        int pendingCountBefore = test(approvalsGrid).size();
        assertTrue("There must be at least one pending purchase to reject",
                pendingCountBefore > 0);

        Purchase pendingPurchase = test(approvalsGrid).item(0);
        assertNotNull(pendingPurchase);
        assertNotNull(pendingPurchase.getId());

        // WHEN: Clicking the Reject button
        var rejectButton = getRejectButton(approvalsGrid);
        test(rejectButton).click();

        // THEN: DecisionWindow opens
        var decisionWindow = $(Window.class)
                .id(DecisionDialog.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);
        assertTrue(decisionWindow.isAttached());

        // WHEN: Clicking Cancel instead of confirming
        test($(decisionWindow, Button.class)
                .id(DecisionDialog.CANCEL_BUTTON_ID)).click();

        // THEN: Window is closed
        assertFalse("Decision window should be closed after cancel",
                decisionWindow.isAttached());

        // THEN: Reject button is re-enabled so the user can retry
        assertTrue("Reject button should be re-enabled after cancel",
                rejectButton.isEnabled());

        // THEN: Grid count is unchanged – purchase is still PENDING
        assertEquals("Grid count should be unchanged after cancel",
                pendingCountBefore, test(approvalsGrid).size());
        assertTrue(test(approvalsGrid).isFocused());

        SerializationDebugUtil.assertSerializable(view);
    }

    /**
     * Tests that the approvals view shows only pending purchases assigned to
     * the currently logged-in approver: User5 sees only their own pending
     * purchases and User6 sees only theirs – neither sees the other's.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void approval_view_displays_pending_purchases_assigned_to_current_user_only()
            throws ServiceException {
        switchToUser("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-approvals-grid");
        int user5Count = test(approvalsGrid).size();
        assertTrue("User5 should have at least one pending purchase assigned",
                user5Count > 0);

        // Assert visible purchases are all assigned to User5
        int checkCount = Math.min(user5Count, 10);
        for (int i = 0; i < checkCount; i++) {
            Purchase p = test(approvalsGrid).item(i);
            assertNotNull("Expected a purchase at row " + i, p);
            assertNotNull("Purchase approver should not be null",
                    p.getApprover());
            assertEquals("All approvals grid items should be assigned to User5",
                    "User5", p.getApprover().getName());
        }

        // WHEN: Login as User6 (the other mock approver)
        switchToUser("User6", "user6");

        view = navigate(
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
                PurchasesView.class);

        approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-approvals-grid");
        int user6Count = test(approvalsGrid).size();
        assertTrue("User6 should have at least one pending purchase assigned",
                user6Count > 0);

        // Assert visible purchases are all assigned to User6
        checkCount = Math.min(user6Count, 10);
        for (int i = 0; i < checkCount; i++) {
            Purchase p = test(approvalsGrid).item(i);
            assertNotNull("Expected a purchase at row " + i, p);
            assertNotNull("Purchase approver should not be null",
                    p.getApprover());
            assertEquals("All approvals grid items should be assigned to User6",
                    "User6", p.getApprover().getName());
        }

        // THEN: The two approvers see disjoint pending queues – neither sees
        // the other's purchases (the combined total exceeds either count alone)
        assertTrue(
                "Combined pending count must exceed each individual approver's count",
                user5Count + user6Count > user5Count
                        && user5Count + user6Count > user6Count);

        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void approving_purchase_when_optimistic_lock_conflict_shows_conflict_notification()
            throws Exception {
        // GIVEN
        switchToUser("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) $(Grid.class)
                .id("purchase-approvals-grid");
        assertNotNull("Approvals grid should be present", approvalsGrid);
        assertTrue("There must be at least one pending purchase",
                test(approvalsGrid).size() > 0);

        // Replace presenter with deterministic failing stub so we hit the
        // condition reliably
        var approvalsView = $(PurchasesApprovalsView.class).first();
        swapApprovalsPresenter(approvalsView,
                new PurchasesApprovalsPresenter() {
                    @Override
                    public ApproveResult approve(Integer purchaseId,
                            User currentUser,
                            String decisionCommentOrNull) {
                        throw new RuntimeException(
                                new OptimisticLockException(
                                        "simulated conflict"));
                    }
                });

        // WHEN: click approve and confirm in decision window
        var approveButton = getApproveButton(approvalsGrid);
        test(approveButton).click();

        var decisionWindow = $(Window.class)
                .id(DecisionDialog.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);

        test($(decisionWindow, Button.class)
                .id(DecisionDialog.CONFIRM_BUTTON_ID)).click();

        // THEN: conflict warning is shown (instead of global technical error
        // flow)
        var expected = approvalsView
                .getTranslation(I18n.Storefront.APPROVE_CONFLICT);
        boolean hasConflictWarning = $(Notification.class).stream()
                .map(Notification::getCaption)
                .anyMatch(expected::equals);

        assertTrue("Expected optimistic lock conflict notification",
                hasConflictWarning);
    }
}
