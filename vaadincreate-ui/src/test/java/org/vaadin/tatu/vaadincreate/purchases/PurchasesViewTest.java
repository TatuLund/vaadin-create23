package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.NumberFormat;
import java.util.Objects;
import java.time.LocalDate;

import javax.persistence.OptimisticLockException;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.jsoup.Jsoup;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.common.CustomChart;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.purchases.PurchaseHistoryGrid.ToggleButton;

import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Window;

/**
 * UI unit tests for PurchasesView.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
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
                PurchasesView.VIEW_NAME + "/" + PurchasesStatsView.VIEW_NAME,
                PurchasesView.class);

        // Wait for async loading to finish (same pattern as StatsViewTest)
        var dashboard = $(CssLayout.class).styleName("dashboard").first();
        assertNotNull("Dashboard layout should be present", dashboard);
        waitForCharts(dashboard);

        // Verify all three chart components exist with stable IDs
        var topChart = $(CustomChart.class).id("purchases-top-products-chart");
        assertNotNull("Top products chart should be present", topChart);

        var leastChart = $(CustomChart.class)
                .id("purchases-least-products-chart");
        assertNotNull("Least products chart should be present", leastChart);

        var monthlyChart = $(CustomChart.class).id("purchases-per-month-chart");
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
        topDataSeries.getData()
                .forEach(item -> assertTrue(
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

        var purchase = test(historyGrid).item(0);
        assertNotNull("Expected a purchase item at row 0", purchase);
        assertFalse("Details should be hidden initially",
                historyGrid.isDetailsVisible(purchase));

        var toggle = (ToggleButton) test(historyGrid).cell(0, 0);
        assertNotNull("Toggle button should be present in first column",
                toggle);

        assertEquals("Open", toggle.getAttribute(AriaAttributes.LABEL));
        assertEquals("false", toggle.getAttribute(AriaAttributes.EXPANDED));
        assertEquals(VaadinIcons.ANGLE_RIGHT, toggle.getIcon());

        // WHEN: Clicking toggle button
        test(toggle).click();

        // THEN: Details are visible and button state updated
        assertTrue(historyGrid.isDetailsVisible(purchase));
        assertEquals("Close", toggle.getAttribute(AriaAttributes.LABEL));
        assertEquals("true", toggle.getAttribute(AriaAttributes.EXPANDED));
        assertEquals(VaadinIcons.ANGLE_DOWN, toggle.getIcon());

        // AND: Details component contains the purchase line items with correct
        // ARIA attributes
        var details = (Label) test(historyGrid).details(0);
        assertPurchaseLineItems(purchase, details);

        // WHEN: Clicking toggle button again
        test(toggle).click();

        // THEN: Details are hidden and button state reverted
        assertTrue(!historyGrid.isDetailsVisible(purchase));
        assertEquals("Open", toggle.getAttribute(AriaAttributes.LABEL));
        assertEquals("false", toggle.getAttribute(AriaAttributes.EXPANDED));
        assertEquals(VaadinIcons.ANGLE_RIGHT, toggle.getIcon());

        SerializationDebugUtil.assertSerializable(view);
    }

    private void assertPurchaseLineItems(Purchase purchase, Label details) {
        assertNotNull("Details component should be present", details);
        var html = details.getValue();

        // Decompose details HTML and verify ARIA + purchase line rendering
        assertNotNull("Details HTML should not be null", html);
        var doc = Jsoup.parse(html);
        var root = doc.getElementsByTag("div").get(0);
        assertEquals("assertive", root.attr(AriaAttributes.LIVE));
        assertEquals(AriaRoles.ALERT, root.attr(AriaAttributes.ROLE));

        assertFalse("Purchase should have at least one line item",
                purchase.getLines().isEmpty());
        NumberFormat euroFormat = EuroConverter.createEuroFormat();
        int index = 0;
        var children = root.getElementsByTag("span");
        for (var line : purchase.getLines()) {
            var productName = line.getProduct().getProductName();
            var unitPrice = euroFormat.format(line.getUnitPrice());
            var quantity = line.getQuantity();
            var lineTotal = euroFormat.format(line.getLineTotal());
            var expectedLine = String.format("%s: %s x %d = %s", productName,
                    unitPrice, quantity, lineTotal);
            assertEquals(expectedLine, children.get(index).text());
            index++;
        }
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

    private Button getApproveButton(Grid<Purchase> approvalsGrid) {
        var approveActionLayout = (HorizontalLayout) test(approvalsGrid).cell(9,
                0);
        return $(approveActionLayout, Button.class).first();
    }

    public static void restoreProductStockLevels(Purchase pendingPurchase) {
        // Restore product stock levels for the approved purchase so that the
        // test has no statistics side effect
        var purchase = PurchaseService.get()
                .fetchPurchaseById(pendingPurchase.getId());
        if (purchase.getStatus() == PurchaseStatus.COMPLETED) {
            purchase.getLines().forEach(line -> {
                var product = ProductDataService.get()
                        .getProductById(line.getProduct().getId());
                var quantity = line.getQuantity();
                product.setStockCount(product.getStockCount() + quantity);
                ProductDataService.get().updateProduct(product);
            });
        }
    }

    private void switchToUser(String name, String password)
            throws ServiceException {
        logout();
        tearDown();
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login(name, password);
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

    private Button getRejectButton(Grid<Purchase> approvalsGrid) {
        var rejectActionLayout = (HorizontalLayout) test(approvalsGrid).cell(9,
                0);
        return $(rejectActionLayout, Button.class).stream().skip(1).findFirst()
                .orElseThrow();
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

    /**
     * Tests that the Purge button is visible and a warning notification is
     * shown when the admin views the purchase history tab and old purchases
     * exist (mock data goes back 800 days; 24-month cutoff ≈ 730 days).
     */
    @Test
    public void purge_button_is_visible_and_notification_shown_when_old_purchases_exist() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var purgeButton = $(Button.class)
                .id(PurchasesHistoryView.PURGE_BUTTON_ID);
        assertNotNull("Purge button should be present", purgeButton);
        assertTrue("Purge button should be enabled when old purchases exist",
                purgeButton.isEnabled());

        // The warning notification about old purchases must be present
        var notifications = $(Notification.class).stream()
                .map(n -> n.getCaption()).filter(c -> c != null).toList();
        boolean hasOldPurchasesNotification = notifications.stream()
                .anyMatch(c -> c.contains("older than 24 months")
                        || c.contains("purchases older"));
        assertTrue(
                "Expected warning notification about old purchases, but got: "
                        + notifications,
                hasOldPurchasesNotification);

        // Spot check that the oldest purchase in the grid has the "old
        // purchase" style
        @SuppressWarnings("unchecked")
        var historyGrid = (Grid<Object>) $(Grid.class)
                .id("purchase-history-grid");
        int last = test(historyGrid).size();
        assertEquals(VaadinCreateTheme.PURCHASE_OLD,
                test(historyGrid).styleName(last - 1));
        assertEquals(VaadinCreateTheme.PURCHASE_OLD,
                test(historyGrid).styleName(last - 10));
        assertEquals(VaadinCreateTheme.PURCHASE_OLD,
                test(historyGrid).styleName(last - 20));

        SerializationDebugUtil.assertSerializable(view);
    }

    /**
     * Tests that clicking Cancel on the purge confirmation dialog closes it
     * without purging: the grid count is unchanged and the Purge button stays
     * visible.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void purge_cancel_closes_dialog_without_purging() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var purgeButton = $(Button.class)
                .id(PurchasesHistoryView.PURGE_BUTTON_ID);
        assertTrue("Purge button should be enabled", purgeButton.isEnabled());

        var historyGrid = (Grid<Object>) $(Grid.class)
                .id("purchase-history-grid");
        int countBefore = test(historyGrid).size();

        // WHEN: Clicking Purge button
        test(purgeButton).click();

        // THEN: confirmation dialog opens (ConfirmDialog uses "confirm-dialog"
        // id)
        var confirmWindow = $(Window.class).id("confirm-dialog");
        assertNotNull("Purge confirmation window should be open",
                confirmWindow);
        assertTrue(confirmWindow.isAttached());

        // WHEN: Clicking Cancel (ConfirmDialog uses "cancel-button" id)
        test($(confirmWindow, Button.class).id("cancel-button")).click();

        // THEN: dialog is closed
        assertFalse("Confirmation window should be closed after cancel",
                confirmWindow.isAttached());

        // THEN: grid count is unchanged
        int countAfter = test(historyGrid).size();
        assertEquals("Grid count should not change after cancel", countBefore,
                countAfter);

        // THEN: purge button is still enabled (old purchases still exist)
        assertTrue("Purge button should still be enabled after cancel",
                purgeButton.isEnabled());

        SerializationDebugUtil.assertSerializable(view);
    }

    /**
     * Tests the full purge workflow: open the purchase history tab → click
     * Purge → confirm → success notification shown → grid refreshed → Purge
     * button hidden (no more old purchases).
     */
    @Test
    @SuppressWarnings("unchecked")
    public void purge_confirm_purges_old_purchases_and_hides_purge_button() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var purgeButton = $(Button.class)
                .id(PurchasesHistoryView.PURGE_BUTTON_ID);
        assertTrue("Purge button should be enabled when old purchases exist",
                purgeButton.isEnabled());

        var historyGrid = (Grid<Object>) $(Grid.class)
                .id("purchase-history-grid");
        int countBefore = test(historyGrid).size();
        assertTrue("Expected purchases in grid", countBefore > 0);

        // WHEN: Clicking Purge and confirming
        test(purgeButton).click();

        // ConfirmDialog uses "confirm-dialog" id for the window
        var confirmWindow = $(Window.class).id("confirm-dialog");
        assertNotNull("Purge confirmation window should be open",
                confirmWindow);

        // ConfirmDialog uses "confirm-button" id for the confirm button
        test($(confirmWindow, Button.class).id("confirm-button")).click();

        // THEN: dialog is closed
        assertFalse("Confirmation window should be closed after confirm",
                confirmWindow.isAttached());

        // THEN: success notification shown
        var successNotification = $(Notification.class).stream()
                .filter(n -> n.getCaption() != null
                        && n.getCaption().contains("deleted"))
                .findFirst().orElse(null);
        assertNotNull("Expected a purge success notification",
                successNotification);

        // THEN: grid shows fewer rows after purge
        int countAfter = test(historyGrid).size();
        assertTrue("Grid should have fewer rows after purging old purchases",
                countAfter < countBefore);

        // THEN: Purge button is now disabled (no more old purchases)
        assertFalse(
                "Purge button should be disabled after all old purchases are removed",
                purgeButton.isEnabled());

        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    public void export_controls_are_present_in_toolbar_order_with_required_date_fields() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var fromDate = $(DateField.class).id(PurchasesHistoryView.FROM_DATE_ID);
        var toDate = $(DateField.class).id(PurchasesHistoryView.TO_DATE_ID);
        var exportButton = $(Button.class)
                .id(PurchasesHistoryView.EXPORT_BUTTON_ID);
        var purgeButton = $(Button.class)
                .id(PurchasesHistoryView.PURGE_BUTTON_ID);

        assertNotNull("From date field should be present", fromDate);
        assertNotNull("To date field should be present", toDate);
        assertNotNull("Export button should be present", exportButton);
        assertNotNull("Purge button should be present", purgeButton);

        assertTrue("From date should have required indicator",
                fromDate.isRequiredIndicatorVisible());
        assertTrue("To date should have required indicator",
                toDate.isRequiredIndicatorVisible());

        assertTrue(test(fromDate).isInteractable());
        assertTrue(test(toDate).isInteractable());
        assertFalse(test(exportButton).isInteractable());
        assertTrue(test(fromDate).isFocused());
    }

    @Test
    public void export_button_enablement_and_date_validation_feedback_follow_rules() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var fromDate = $(DateField.class).id(PurchasesHistoryView.FROM_DATE_ID);
        var toDate = $(DateField.class).id(PurchasesHistoryView.TO_DATE_ID);
        var exportButton = $(Button.class)
                .id(PurchasesHistoryView.EXPORT_BUTTON_ID);

        assertFalse("Export starts disabled with missing dates",
                exportButton.isEnabled());

        var from = LocalDate.now().minusDays(5);
        test(fromDate).setValue(from);
        assertFalse("Export stays disabled when only from is set",
                exportButton.isEnabled());

        test(toDate).setValue(from.minusDays(1));
        assertFalse("Export disabled when to < from", exportButton.isEnabled());
        assertNotNull("To date should have component error for invalid range",
                toDate.getComponentError());

        test(toDate).setValue(from.plusMonths(3).plusDays(1));
        assertFalse("Export disabled when range is over three months",
                exportButton.isEnabled());
        assertNotNull("To date should have component error for max range",
                toDate.getComponentError());

        test(toDate).setValue(from.plusMonths(3));
        assertTrue("Export enabled when range is exactly three months",
                exportButton.isEnabled());
        assertNull("To date component error should clear for valid range",
                toDate.getComponentError());
    }

    @Test
    public void export_date_fields_disallow_future_dates_with_range_end_today() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var fromDate = $(DateField.class).id(PurchasesHistoryView.FROM_DATE_ID);
        var toDate = $(DateField.class).id(PurchasesHistoryView.TO_DATE_ID);

        assertEquals("From date range end should be today", LocalDate.now(),
                fromDate.getRangeEnd());
        assertEquals("To date range end should be today", LocalDate.now(),
                toDate.getRangeEnd());
    }

    @Test
    public void export_dialog_is_shown_when_export_button_clicked_with_valid_dates() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var fromDate = $(DateField.class).id(PurchasesHistoryView.FROM_DATE_ID);
        var toDate = $(DateField.class).id(PurchasesHistoryView.TO_DATE_ID);
        var exportButton = $(Button.class)
                .id(PurchasesHistoryView.EXPORT_BUTTON_ID);

        var from = LocalDate.now().minusDays(10);
        var to = LocalDate.now().minusDays(5);
        test(fromDate).setValue(from);
        test(toDate).setValue(to);

        assertTrue("Export button should be enabled with valid dates",
                exportButton.isEnabled());

        // WHEN: Clicking Export
        test(exportButton).click();
        assertFalse(test(exportButton).isInteractable());

        waitWhile(Window.class,
                Void -> $(Window.class).id("purchase-export-dialog") == null,
                1);

        // THEN: Export dialog is shown
        var exportDialog = $(Window.class).id("purchase-export-dialog");
        assertNotNull("Export dialog should be open", exportDialog);
        assertEquals("Export", exportDialog.getCaption());

        var downloadButton = $(exportDialog, Button.class)
                .id("export-download-button");
        assertTrue(test(downloadButton).isFocused());

        exportDialog.close();
        assertTrue(test(exportButton).isInteractable());
        assertTrue(test($(Grid.class).id("purchase-history-grid")).isFocused());
    }

    @Test
    public void export_range_is_highlighted_with_styled_rows_in_grid() {
        view = navigate(PurchasesView.VIEW_NAME, PurchasesView.class);

        var fromDate = $(DateField.class).id(PurchasesHistoryView.FROM_DATE_ID);
        var toDate = $(DateField.class).id(PurchasesHistoryView.TO_DATE_ID);
        var historyGrid = $(Grid.class).id("purchase-history-grid");

        var from = LocalDate.now().minusDays(10);
        var to = LocalDate.now();
        test(fromDate).setValue(from);
        test(toDate).setValue(to);

        // THEN: Rows in the grid that fall within the selected date range have
        // the "in-range" style, and rows outside the range do not
        boolean hasInRange = false;
        boolean hasOutOfRange = false;
        for (int i = 0; i < 100; i++) {
            @SuppressWarnings("unchecked")
            String style = test(historyGrid).styleName(i);
            if (Objects.equals(style, VaadinCreateTheme.PURCHASE_RANGE)) {
                hasInRange = true;
            } else {
                hasOutOfRange = true;
            }
            if (hasInRange && hasOutOfRange) {
                break; // no need to check further if we have both cases
            }
        }
        assertTrue(
                "Expected at least one in-range purchase with correct styling",
                hasInRange);
        assertTrue(
                "Expected at least one out-of-range purchase without in-range styling",
                hasOutOfRange);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void approving_purchase_when_optimistic_lock_conflict_shows_conflict_notification()
            throws Exception {
        // GIVEN
        switchToUser("User5", "user5");

        view = navigate(
                PurchasesView.VIEW_NAME + "/"
                        + PurchasesApprovalsView.VIEW_NAME,
                PurchasesView.class);

        var approvalsGrid = (Grid<Purchase>) (Grid) $(Grid.class)
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
                .id(DecisionDialog.CONFIRM_BUTTON_ID))
                .click();

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

    private static void swapApprovalsPresenter(
            PurchasesApprovalsView approvalsView,
            PurchasesApprovalsPresenter replacement) throws Exception {
        var field = PurchasesApprovalsView.class.getDeclaredField("presenter");
        field.setAccessible(true);
        field.set(approvalsView, replacement);
    }
}
