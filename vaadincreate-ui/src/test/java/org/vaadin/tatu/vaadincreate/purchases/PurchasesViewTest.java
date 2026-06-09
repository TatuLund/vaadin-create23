package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.NumberFormat;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.jsoup.Jsoup;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.common.CustomChart;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.purchases.PurchaseHistoryGrid.ToggleButton;

import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;

/**
 * UI unit tests for PurchasesView.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PurchasesViewTest extends AbstractPurchasesTest {

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
        // Status, Ordered At, Decided At, Supervisor, and Comments
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
}
