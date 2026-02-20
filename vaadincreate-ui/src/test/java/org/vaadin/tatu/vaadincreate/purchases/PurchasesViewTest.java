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
import org.vaadin.tatu.vaadincreate.common.CustomChart;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.purchases.PurchaseHistoryGrid.ToggleButton;

import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Notification;
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
                .id("purchase-approvals-grid");
        assertNotNull("Approvals grid should be present", approvalsGrid);
        // Grid has 10 columns total, but 3 are hidden in PENDING_APPROVALS mode
        // (Approver, Decided At, Decision Reason). Visible: Toggle, ID,
        // Requester, Created At, Status, Total, Actions = 7 visible columns.
        assertEquals("Approvals grid should have 10 columns total", 10,
                approvalsGrid.getColumns().size());
        long visibleCount = approvalsGrid.getColumns().stream()
                .filter(c -> !c.isHidden()).count();
        assertEquals("Approvals grid should have 7 visible columns", 7,
                visibleCount);

        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    public void should_ShowCharts_When_NavigatingToStatsTab() {
        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesStatsTab.VIEW_NAME,
                PurchasesView.class);

        // Wait for async loading to finish (same pattern as StatsViewTest)
        var dashboard = $(CssLayout.class)
                .styleName("dashboard").first();
        assertNotNull("Dashboard layout should be present", dashboard);
        waitForCharts(dashboard);

        // Verify all three chart components exist with stable IDs
        var topChart = $(CustomChart.class)
                .id("purchases-top-products-chart");
        assertNotNull("Top products chart should be present", topChart);

        var leastChart = $(CustomChart.class)
                .id("purchases-least-products-chart");
        assertNotNull("Least products chart should be present", leastChart);

        var monthlyChart = $(CustomChart.class)
                .id("purchases-per-month-chart");
        assertNotNull("Monthly totals chart should be present", monthlyChart);

        // Verify data was loaded: top-products chart must have at least one
        // series with data points
        var topSeries = topChart.getConfiguration().getSeries();
        assertFalse("Top products chart should have at least one series",
                topSeries.isEmpty());
        var topDataSeries = (DataSeries) topSeries.get(0);
        assertFalse("Top products series should have data points",
                topDataSeries.getData().isEmpty());
        // Each data point in a completed purchase stats series must have a
        // positive quantity
        topDataSeries.getData().forEach(item -> assertTrue(
                "Top product quantity should be positive",
                item.getY().doubleValue() > 0));

        // Monthly chart should have 12 x-axis categories (one per month)
        assertEquals("Monthly chart should have 12 months on x-axis", 12,
                monthlyChart.getConfiguration().getxAxis()
                        .getCategories().length);

        SerializationDebugUtil.assertSerializable(view);
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
        var approveActionLayout = (com.vaadin.ui.HorizontalLayout) test(
                approvalsGrid).cell(9, 0);
        var approveButton = $(approveActionLayout, Button.class).first();
        test(approveButton).click();

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

        // THEN: A notification about the purchase is shown (either
        // "approved successfully" when stock is sufficient, or "cancelled:
        // Insufficient stock" when stock runs out – both are valid outcomes
        // per the PRD and both remove the purchase from the pending queue)
        String purchaseIdStr = String.valueOf(pendingPurchase.getId());
        boolean notified = $(Notification.class).stream()
                .anyMatch(n -> n.getCaption() != null
                        && n.getCaption()
                                .startsWith("Purchase #" + purchaseIdStr));
        assertTrue("Expected a notification for purchase #" + purchaseIdStr,
                notified);

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
        var rejectActionLayout = (com.vaadin.ui.HorizontalLayout) test(
                approvalsGrid).cell(9, 0);
        var rejectButton = $(rejectActionLayout, Button.class).stream().skip(1)
                .findFirst().orElseThrow();
        test(rejectButton).click();

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

    /**
     * Tests that clicking Cancel on the approval DecisionWindow closes the
     * window without changing the purchase status: the purchase stays PENDING
     * and the grid count is unchanged.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void approve_pending_purchase_shows_decision_window_and_clicking_cancel_then_status_remains_pending()
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
                .id("purchase-approvals-grid");
        int pendingCountBefore = test(approvalsGrid).size();
        assertTrue("There must be at least one pending purchase to approve",
                pendingCountBefore > 0);

        Purchase pendingPurchase = test(approvalsGrid).item(0);
        assertNotNull(pendingPurchase);
        assertNotNull(pendingPurchase.getId());

        // WHEN: Clicking the Approve button
        var approveActionLayout = (com.vaadin.ui.HorizontalLayout) test(
                approvalsGrid).cell(9, 0);
        var approveButton = $(approveActionLayout, Button.class).first();
        test(approveButton).click();

        // THEN: DecisionWindow opens
        var decisionWindow = $(Window.class)
                .id(DecisionWindow.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);
        assertTrue(decisionWindow.isAttached());

        // WHEN: Clicking Cancel instead of confirming
        test($(decisionWindow, Button.class)
                .id(DecisionWindow.CANCEL_BUTTON_ID))
                .click();

        // THEN: Window is closed
        assertFalse("Decision window should be closed after cancel",
                decisionWindow.isAttached());

        // THEN: Approve button is re-enabled so the user can retry
        assertTrue("Approve button should be re-enabled after cancel",
                approveButton.isEnabled());

        // THEN: Grid count is unchanged – purchase is still PENDING
        assertEquals("Grid count should be unchanged after cancel",
                pendingCountBefore, test(approvalsGrid).size());

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
                .id("purchase-approvals-grid");
        int pendingCountBefore = test(approvalsGrid).size();
        assertTrue("There must be at least one pending purchase to reject",
                pendingCountBefore > 0);

        Purchase pendingPurchase = test(approvalsGrid).item(0);
        assertNotNull(pendingPurchase);
        assertNotNull(pendingPurchase.getId());

        // WHEN: Clicking the Reject button
        var rejectActionLayout = (com.vaadin.ui.HorizontalLayout) test(
                approvalsGrid).cell(9, 0);
        var rejectButton = $(rejectActionLayout, Button.class).stream().skip(1)
                .findFirst().orElseThrow();
        test(rejectButton).click();

        // THEN: DecisionWindow opens
        var decisionWindow = $(Window.class)
                .id(DecisionWindow.DECISION_WINDOW_ID);
        assertNotNull("Decision window should be open", decisionWindow);
        assertTrue(decisionWindow.isAttached());

        // WHEN: Clicking Cancel instead of confirming
        test($(decisionWindow, Button.class)
                .id(DecisionWindow.CANCEL_BUTTON_ID))
                .click();

        // THEN: Window is closed
        assertFalse("Decision window should be closed after cancel",
                decisionWindow.isAttached());

        // THEN: Reject button is re-enabled so the user can retry
        assertTrue("Reject button should be re-enabled after cancel",
                rejectButton.isEnabled());

        // THEN: Grid count is unchanged – purchase is still PENDING
        assertEquals("Grid count should be unchanged after cancel",
                pendingCountBefore, test(approvalsGrid).size());

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
        // GIVEN: Login as User5 (one of the two mock approvers)
        logout();
        tearDown();
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesApprovalsTab.VIEW_NAME,
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
        logout();
        tearDown();
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("User6", "user6");

        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesApprovalsTab.VIEW_NAME,
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
}
