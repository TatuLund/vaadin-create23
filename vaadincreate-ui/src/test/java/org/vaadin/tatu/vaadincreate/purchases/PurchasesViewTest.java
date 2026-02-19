package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.purchases.PurchaseHistoryGrid.ToggleButton;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Window;

/**
 * UI unit tests for PurchasesView.
 */
public class PurchasesViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private PurchasesView view;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_ShowHistoryGrid_When_ViewIsDisplayed() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        assertNotNull(view);
        assertAssistiveNotification("Purchases opened");
        assertAssistiveNotification("Purchase History opened");

        var historyGrid = (Grid<Object>) $(Grid.class)
                .id("purchase-history-grid");
        assertNotNull("Purchase history grid should be present", historyGrid);
        assertTrue("Purchase history grid should have at least one row",
                historyGrid.getDataCommunicator().getDataProviderSize() > 0);
        // Assert 9 columns are present: Toggle, Product, Category, Price,
        // Status,
        // Ordered At, Decided At, Supervisor, and Comments
        assertEquals("Purchase history grid should have 9 columns", 9,
                historyGrid.getColumns().size());

        var item = test(historyGrid).item(1000);
        assertNotNull("Purchase history grid should contain item at row 1000",
                item);

        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_ShowApprovalsGrid_When_NavigatingToApprovalsTab() {
        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesApprovalsTab.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Object>) $(Grid.class)
                .id("purchase-history-grid");
        assertNotNull("Approvals grid should be present", approvalsGrid);
        // Grid has 10 columns: Toggle, ID, Requester, Approver, Created At,
        // Status, Decided At, Total, Decision Reason, Actions
        assertEquals("Approvals grid should have 10 columns", 10,
                approvalsGrid.getColumns().size());

        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    public void should_ShowStatsPlaceholder_When_NavigatingToStatsTab() {
        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesStatsTab.VIEW_NAME,
                PurchasesView.class);

        var placeholder = $(Label.class).id("purchases-stats-placeholder");
        assertEquals("Statistics", placeholder.getValue());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void clicking_toggle_button_toggles_details_visibility_and_updates_aria_and_icon() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var historyGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-history-grid");
        assertTrue("Purchase history grid should have at least one row",
                test(historyGrid).size() > 0);

        Purchase purchase = test(historyGrid).item(0);
        assertNotNull("Expected a purchase item at row 0", purchase);
        assertTrue("Details should be hidden initially",
                !historyGrid.isDetailsVisible(purchase));

        var toggle = (ToggleButton) test(historyGrid).cell(0, 0);
        assertNotNull("Toggle button should be present in first column",
                toggle);

        assertEquals("Open",
                toggle.getAttribute(AriaAttributes.LABEL));
        assertEquals("false", toggle.getAttribute(AriaAttributes.EXPANDED));
        assertEquals(VaadinIcons.ANGLE_RIGHT, toggle.getIcon());

        // WHEN: Clicking toggle button
        test(toggle).click();

        // THEN: Details are visible and button state updated
        assertTrue(historyGrid.isDetailsVisible(purchase));
        assertEquals("Close",
                toggle.getAttribute(AriaAttributes.LABEL));
        assertEquals("true", toggle
                .getAttribute(AriaAttributes.EXPANDED));
        assertEquals(VaadinIcons.ANGLE_DOWN, toggle.getIcon());

        // WHEN: Clicking toggle button again
        test(toggle).click();

        // THEN: Details are hidden and button state reverted
        assertTrue(!historyGrid.isDetailsVisible(purchase));
        assertEquals("Open",
                toggle.getAttribute(AriaAttributes.LABEL));
        assertEquals("false", toggle
                .getAttribute(AriaAttributes.EXPANDED));
        assertEquals(VaadinIcons.ANGLE_RIGHT, toggle.getIcon());

        SerializationDebugUtil.assertSerializable(view);
    }

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
        logout();
        tearDown();
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesApprovalsTab.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-history-grid");
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
        test($(Button.class).id("approve-button-" + pendingPurchase.getId()))
                .click();

        // THEN: DecisionWindow opens
        var decisionWindow = $(Window.class)
                .id(DecisionWindow.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);
        assertTrue(decisionWindow.isAttached());

        // WHEN: Entering an optional comment and confirming
        var commentField = $(decisionWindow, TextArea.class)
                .id(DecisionWindow.DECISION_COMMENT_ID);
        assertNotNull("Comment field should be present", commentField);
        test(commentField).setValue("Approved by supervisor");

        test($(decisionWindow, Button.class)
                .id(DecisionWindow.CONFIRM_BUTTON_ID))
                .click();

        // THEN: Window is closed
        assertFalse("Decision window should be closed after confirm",
                decisionWindow.isAttached());

        // THEN: Success notification is shown
        assertNotification(
                "Purchase #" + pendingPurchase.getId()
                        + " approved successfully");

        // THEN: Approved purchase no longer appears in the pending approvals
        // grid (status changed to COMPLETED)
        int pendingCountAfter = test(approvalsGrid).size();
        assertEquals(
                "Approved purchase should be removed from the pending approvals grid",
                pendingCountBefore - 1, pendingCountAfter);

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
        // GIVEN: Login as User5 who is a designated approver in the mock data
        logout();
        tearDown();
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesApprovalsTab.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-history-grid");
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
        test($(Button.class).id("reject-button-" + pendingPurchase.getId()))
                .click();

        // THEN: DecisionWindow opens
        var decisionWindow = $(Window.class)
                .id(DecisionWindow.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);
        assertTrue(decisionWindow.isAttached());

        // THEN: Confirm (reject) button is initially disabled – reason required
        var confirmButton = $(decisionWindow, Button.class)
                .id(DecisionWindow.CONFIRM_BUTTON_ID);
        assertFalse("Confirm button should be disabled until reason is entered",
                confirmButton.isEnabled());

        // WHEN: Entering a rejection reason
        var commentField = $(decisionWindow, TextArea.class)
                .id(DecisionWindow.DECISION_COMMENT_ID);
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
}
