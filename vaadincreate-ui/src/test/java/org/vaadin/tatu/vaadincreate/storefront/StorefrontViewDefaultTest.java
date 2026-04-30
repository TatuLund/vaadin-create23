package org.vaadin.tatu.vaadincreate.storefront;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.NumberFormat;

import org.jsoup.Jsoup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.events.PurchaseStatusChangedEvent;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.common.NumberField;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.purchases.PurchasesViewTest;

import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;

public class StorefrontViewDefaultTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private StorefrontView view;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        resetStatusCheck();
        login("Customer11", "customer11");
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void should_ShowStorefrontView_When_CustomerAccesses() {
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        assertNotNull(view);
        assertNotification("Status Updates");
        assertAssistiveNotification("Storefront opened");
        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void clicking_grid_row_toggles_details_visibility() {
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        var historyGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-history-grid");
        assertTrue("Purchase history grid should have at least one row",
                test(historyGrid).size() > 0);

        int row = findFirstPurchaseRow(historyGrid, PurchaseStatus.COMPLETED);
        assertTrue("Expected a completed purchase row", row >= 0);

        var purchase = test(historyGrid).item(row);
        assertNotNull("Expected a purchase item at the completed row",
                purchase);
        assertFalse("Details should be hidden initially",
                historyGrid.isDetailsVisible(purchase));

        test(historyGrid).click(0, row);

        assertTrue(historyGrid.isDetailsVisible(purchase));

        var details = (Label) test(historyGrid).details(row);
        assertPurchaseLineItems(purchase, details);

        test(historyGrid).click(0, row);

        assertFalse(historyGrid.isDetailsVisible(purchase));
        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_PrefillAddressAndSupervisor_FromLatestPurchase() {
        var latestPurchase = PurchaseService.get()
                .findMyPurchases(getCurrentCustomer(), 0, 1).get(0);
        var expectedAddress = latestPurchase.getDeliveryAddress();
        var expectedSupervisor = latestPurchase.getApprover();
        assertNotNull("Latest purchase should have an approver",
                expectedSupervisor);

        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);

        var numberField = getNumberField(productGrid);
        test(numberField).setValue(1);

        var nextButton = $(Button.class).caption("Next").first();
        test(nextButton).click();

        assertEquals(expectedAddress.getStreet(),
                $(TextField.class).caption("Street").first().getValue());
        assertEquals(expectedAddress.getPostalCode(),
                $(TextField.class).caption("Postal Code").first().getValue());
        assertEquals(expectedAddress.getCity(),
                $(TextField.class).caption("City").first().getValue());
        assertEquals(expectedAddress.getCountry(),
                $(TextField.class).caption("Country").first().getValue());

        test(nextButton).click();

        var supervisorCombo = (ComboBox<User>) $(ComboBox.class).first();
        var selectedSupervisor = supervisorCombo.getValue();
        assertNotNull("Supervisor should be preselected", selectedSupervisor);
        assertEquals(expectedSupervisor.getId(), selectedSupervisor.getId());

        test(nextButton).click();
        assertNotNull("Expected review step to be shown with defaults applied",
                $(Button.class).caption("Submit").first());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void purchase_history_grid_refreshes_when_purchase_updated_event_is_fired() {
        var pendingPurchase = createPendingPurchaseForCurrentCustomer();

        // GIVEN: Storefront view is displayed
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);
        $(Notification.class).forEach(Notification::close);
        var historyGrid = (Grid<Purchase>) $(Grid.class)
                .id("purchase-history-grid");
        assertNotNull("Purchase history grid should be present", historyGrid);
        int initialSize = test(historyGrid).size();
        assertTrue("Purchase history should have at least one entry",
                initialSize > 0);

        // WHEN: A PurchaseUpdatedEvent is posted for a purchase owned by the
        // current user (simulating that another session updated a purchase)
        int row = findPurchaseRowById(historyGrid, pendingPurchase.getId());
        assertTrue("Expected a pending purchase row for the current customer",
                row >= 0);
        assertEquals(PurchaseStatus.PENDING.toString(),
                test(historyGrid).cell(5, row).toString());

        // This simulates superviser approving the purchase in another session,
        // which triggers a PurchaseStatusChangedEvent that the grid listens to
        // and should refresh the item
        User approver = pendingPurchase.getApprover();
        assertNotNull("Pending purchase should have an approver", approver);
        PurchaseService.get().approve(pendingPurchase.getId(),
                approver, "Looks good");
        EventBus.get()
                .post(new PurchaseStatusChangedEvent(pendingPurchase.getId()));

        final int finalRow = row;
        waitWhile(() -> test(historyGrid).cell(5, finalRow).toString()
                .equals("PENDING"), 1);

        // THEN: Grid size is unchaged
        int sizeAfterEvent = test(historyGrid).size();
        assertEquals(
                "Grid size should not increase after PurchaseStatusChangedEvent",
                initialSize, sizeAfterEvent);
        // AND: The updated purchase should have the new status
        assertNotEquals(PurchaseStatus.PENDING.toString(),
                test(historyGrid).cell(5, row).toString());
        // AND: A notification about the status change should be shown
        assertNotNull($(Notification.class).last());

        PurchasesViewTest.restoreProductStockLevels(pendingPurchase);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void purchase_history_grid_refreshes_when_wizard_is_submitted() {
        // GIVEN: Storefront view is displayed
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);
        var historyGrid = (Grid<Purchase>) $(Grid.class)
                .id("purchase-history-grid");
        assertNotNull("Purchase history grid should be present", historyGrid);
        int initialSize = test(historyGrid).size();

        // WHEN: User completes and submits the purchase wizard
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);
        var numberField = getNumberField(productGrid);
        test(numberField).setValue(1);
        var nextButton = $(Button.class).caption("Next").first();
        test(nextButton).click();
        test($(TextField.class).caption("Street").first())
                .setValue("123 Main St");
        test($(TextField.class).caption("Postal Code").first())
                .setValue("12345");
        test($(TextField.class).caption("City").first()).setValue("TestCity");
        test($(TextField.class).caption("Country").first())
                .setValue("TestCountry");
        test(nextButton).click();
        var supervisorCombo = (ComboBox<Object>) $(ComboBox.class).first();
        test(supervisorCombo).clickItem(supervisorCombo.getDataCommunicator()
                .fetchItemsWithRange(0, 1).get(0));
        test(nextButton).click();
        var submitButton = $(Button.class).caption("Submit").first();
        test(submitButton).click();

        // THEN: Success notification is shown and history grid has a new entry
        assertTrue("Success notification should be shown",
                $(Notification.class).stream()
                        .anyMatch(n -> n.getCaption() != null
                                && n.getCaption().contains("created")));

        waitWhile(() -> initialSize == test(historyGrid).size(), 1);

        var latestPurchase = PurchaseService.get()
                .findMyPurchases(getCurrentCustomer(), 0, 1).get(0);
        int row = findPurchaseRowById(historyGrid, latestPurchase.getId());
        int sizeAfterSubmit = test(historyGrid).size();
        assertTrue(
                "Purchase history grid should have more entries after a new purchase is created",
                sizeAfterSubmit > initialSize);
        assertTrue("Expected submitted purchase to appear in history grid",
                row >= 0);
        assertEquals(PurchaseStatus.PENDING.toString(),
                test(historyGrid).cell(5, row).toString());
    }

    private Purchase createPendingPurchaseForCurrentCustomer() {
        var cart = new Cart();
        var product = ui.getProductService().getAllProducts().iterator().next();
        cart.addItem(product, 1);
        return PurchaseService.get().createPendingPurchase(cart,
                new Address("Test St", "00100", "Helsinki", "Finland"),
                getCurrentCustomer(),
                ui.getUserService().findByName("User8").orElseThrow());
    }

    private int findPurchaseRowById(Grid<Purchase> historyGrid,
            Integer purchaseId) {
        int size = test(historyGrid).size();
        for (int row = 0; row < size; row++) {
            Purchase purchase = test(historyGrid).item(row);
            if (purchase != null && purchaseId.equals(purchase.getId())) {
                return row;
            }
        }
        return -1;
    }

    private NumberField getNumberField(Grid<ProductDto> productGrid) {
        return $((HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
    }

    private User getCurrentCustomer() {
        return ui.getUserService().findByName("Customer11").orElseThrow();
    }

    private void resetStatusCheck() {
        var customer = getCurrentCustomer();
        customer.setLastStatusCheck(null);
        ui.getUserService().updateUser(customer);
    }

    private int findFirstPurchaseRow(Grid<Purchase> historyGrid,
            PurchaseStatus status) {
        int size = test(historyGrid).size();
        for (int row = 0; row < size; row++) {
            Purchase purchase = test(historyGrid).item(row);
            if (purchase != null && status.equals(purchase.getStatus())) {
                return row;
            }
        }
        return -1;
    }

    private void assertPurchaseLineItems(Purchase purchase, Label details) {
        assertNotNull("Details component should be present", details);
        var html = details.getValue();

        assertNotNull("Details HTML should not be null", html);
        var doc = Jsoup.parse(html);
        var root = doc.getElementsByTag("div").get(0);
        assertEquals("assertive", root.attr(AriaAttributes.LIVE));
        assertEquals(AriaRoles.ALERT, root.attr(AriaAttributes.ROLE));

        assertFalse("Purchase should have at least one line item",
                purchase.getLines().isEmpty());
        NumberFormat euroFormat = EuroConverter.createEuroFormat();
        var children = root.getElementsByTag("span");

        assertEquals(": " + purchase.getId(), children.get(0).text());
        assertEquals(": " + purchase.getApprover().getName(),
                children.get(1).text());
        assertEquals(": " + purchase.getDecisionReason(),
                children.get(3).text());

        int index = 4;
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
