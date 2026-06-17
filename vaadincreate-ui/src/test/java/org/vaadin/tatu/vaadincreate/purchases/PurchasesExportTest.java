package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Objects;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;

import com.vaadin.ui.Button;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Window;

public class PurchasesExportTest extends AbstractPurchasesTest {

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

        var from = LocalDate.now().minusMonths(4);
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
    @SuppressWarnings("unchecked")
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

        waitWhile(() -> $(Window.class).size() == 0, 1);

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
}
