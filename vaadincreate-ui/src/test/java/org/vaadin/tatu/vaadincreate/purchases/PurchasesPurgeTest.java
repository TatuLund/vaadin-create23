package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;

import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Window;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PurchasesPurgeTest extends AbstractPurchasesTest {

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
}
