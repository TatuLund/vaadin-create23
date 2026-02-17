package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;

import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;

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
        // Assert 9 columns are present: Toggle, Product, Category, Price, Status,
        // Ordered At, Decided At, Supervisor, and Comments
        assertEquals("Purchase history grid should have 9 columns", 9,
                historyGrid.getColumns().size());

        var item = test(historyGrid).item(1000);
        assertNotNull("Purchase history grid should contain item at row 1000",
                item);

        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    public void should_ShowApprovalsPlaceholder_When_NavigatingToApprovalsTab() {
        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesApprovalsTab.VIEW_NAME,
                PurchasesView.class);

        var placeholder = $(Label.class)
                .id("purchases-approvals-placeholder");
        assertEquals("Approvals", placeholder.getValue());
    }

    @Test
    public void should_ShowStatsPlaceholder_When_NavigatingToStatsTab() {
        view = navigate(
                PurchasesView.VIEW_NAME + "/" + PurchasesStatsTab.VIEW_NAME,
                PurchasesView.class);

        var placeholder = $(Label.class).id("purchases-stats-placeholder");
        assertEquals("Statistics", placeholder.getValue());
    }
}
