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

import com.vaadin.shared.Position;
import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;

/**
 * UI unit tests for StorefrontView and PurchaseWizard that require existing
 * purchase data (mock data generated for Customer11). Uses Customer11 who has
 * previous purchases with a default address and supervisor.
 */
public class StorefrontViewDefaultTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private StorefrontView view;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("Customer11", "customer11");
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void should_ShowStorefrontView_When_CustomerAccesses() {
        // GIVEN: User is logged in as CUSTOMER
        // WHEN: Navigate to storefront view
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // THEN: View should be visible
        assertNotNull(view);
        assertNotification("Status Updates");
        assertAssistiveNotification("Storefront opened");

        // AND: View should be serializable
        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    public void clicking_grid_row_toggles_details_visibility() {
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        var historyGrid = (Grid<Purchase>) (Grid) $(Grid.class)
                .id("purchase-history-grid");
        assertTrue("Purchase history grid should have at least one row",
                test(historyGrid).size() > 0);

        int row = findFirstPurchaseRow(historyGrid, PurchaseStatus.COMPLETED);

        var purchase = test(historyGrid).item(row);
        assertNotNull("Expected a purchase item at row 0", purchase);
        assertFalse("Details should be hidden initially",
                historyGrid.isDetailsVisible(purchase));

        // WHEN: Clicking toggle button
        test(historyGrid).click(0, row);

        // THEN: Details are visible
        assertTrue(historyGrid.isDetailsVisible(purchase));

        // AND: Details component contains the purchase line items with correct
        // ARIA attributes
        var details = (Label) test(historyGrid).details(row);
        assertPurchaseLineItems(purchase, details);

        // WHEN: Clicking toggle button again
        test(historyGrid).click(0, row);

        // THEN: Details are hidden
        assertFalse(historyGrid.isDetailsVisible(purchase));

        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void purchase_history_grid_refreshes_when_purchase_updated_event_is_fired() {
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
        int row = findFirstPurchaseRow(historyGrid, PurchaseStatus.PENDING);
        assertEquals("PENDING", test(historyGrid).cell(5, row).toString());

        // This simulates superviser approving the purchase in another session,
        // which triggers a PurchaseStatusChangedEvent that the grid listens to
        // and should refresh the item
        Purchase pendingPurchase = test(historyGrid).item(row);
        PurchaseService.get().approve(pendingPurchase.getId(),
                pendingPurchase.getApprover(), "Looks good");
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
        assertNotEquals("PENDING", test(historyGrid).cell(5, row).toString());
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

        int row = findFirstPurchaseRow(historyGrid, PurchaseStatus.PENDING);
        int sizeAfterSubmit = test(historyGrid).size();
        assertTrue(
                "Purchase history grid should have more entries after a new purchase is created",
                sizeAfterSubmit > initialSize);
        assertEquals("PENDING", test(historyGrid).cell(5, row).toString());
    }

    @Test
    public void should_PrefillDefaultAddress_When_RequesterHasPreviousPurchases() {
        // GIVEN: Storefront view displayed with Customer11 (has previous
        // purchases)
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        @SuppressWarnings("unchecked")
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);

        var numberField = getNumberField(productGrid);
        test(numberField).setValue(1);

        var nextButton = $(Button.class).caption("Next").first();
        test(nextButton).click();

        // THEN: Street field should be pre-filled from last purchase address
        var streetField = $(TextField.class).caption("Street").first();
        String expectedStreet = streetField.getValue();
        assertNotNull("Street should be prefilled", expectedStreet);
        assertFalse("Street should not be empty", expectedStreet.isEmpty());

        // AND: Postal code should be pre-filled
        var postalCodeField = $(TextField.class).caption("Postal Code").first();
        String expectedPostalCode = postalCodeField.getValue();
        assertNotNull("Postal code should be prefilled", expectedPostalCode);
        assertFalse("Postal code should not be empty",
                expectedPostalCode.isEmpty());

        // AND: City should be pre-filled
        var cityField = $(TextField.class).caption("City").first();
        String expectedCity = cityField.getValue();
        assertNotNull("City should be prefilled", expectedCity);
        assertFalse("City should not be empty", expectedCity.isEmpty());

        // AND: Country should be pre-filled
        var countryField = $(TextField.class).caption("Country").first();
        String expectedCountry = countryField.getValue();
        assertNotNull("Country should be prefilled", expectedCountry);
        assertFalse("Country should not be empty", expectedCountry.isEmpty());

        // Cleanup: fill remaining fields to proceed past step 2
        test($(TextField.class).caption("Street").first())
                .setValue(expectedStreet);
        test(postalCodeField).setValue(expectedPostalCode);
        test(cityField).setValue(expectedCity);
        test(countryField).setValue(expectedCountry);
    }

    @Test
    public void should_PrefillDefaultSupervisor_When_RequesterHasPreviousPurchases() {
        // GIVEN: Storefront view displayed with Customer11 (has previous
        // purchases)
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        @SuppressWarnings("unchecked")
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);

        var numberField = getNumberField(productGrid);
        test(numberField).setValue(1);

        var nextButton = $(Button.class).caption("Next").first();
        test(nextButton).click();

        // Fill address to proceed from step 2
        test($(TextField.class).caption("Street").first())
                .setValue("123 Main St");
        test($(TextField.class).caption("Postal Code").first())
                .setValue("12345");
        test($(TextField.class).caption("City").first()).setValue("TestCity");
        test($(TextField.class).caption("Country").first())
                .setValue("TestCountry");
        test(nextButton).click();

        // THEN: Supervisor combobox should have a default selected from last
        // purchase
        @SuppressWarnings("unchecked")
        var supervisorCombo = (ComboBox<User>) $(ComboBox.class).first();
        assertNotNull("Supervisor combobox should be present", supervisorCombo);
        User selectedValue = supervisorCombo.getValue();
        assertNotNull("Default supervisor should be pre-selected",
                selectedValue);
    }

    @Test
    public void should_PrefillAddressAndSupervisor_FromSameLastPurchase() {
        // GIVEN: Storefront view displayed with Customer11 (has previous
        // purchases)
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        @SuppressWarnings("unchecked")
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);

        var numberField = getNumberField(productGrid);
        test(numberField).setValue(1);

        var nextButton = $(Button.class).caption("Next").first();
        test(nextButton).click();

        // Capture address fields prefill from step 2
        var streetField = $(TextField.class).caption("Street").first();
        String prefilledStreet = streetField.getValue();
        var postalCodeField = $(TextField.class).caption("Postal Code").first();
        String prefilledPostalCode = postalCodeField.getValue();
        var cityField = $(TextField.class).caption("City").first();
        String prefilledCity = cityField.getValue();
        var countryField = $(TextField.class).caption("Country").first();
        String prefilledCountry = countryField.getValue();

        // Fill address to proceed from step 2
        test($(TextField.class).caption("Street").first())
                .setValue(prefilledStreet);
        test(postalCodeField).setValue(prefilledPostalCode);
        test(cityField).setValue(prefilledCity);
        test(countryField).setValue(prefilledCountry);
        test(nextButton).click();

        // Capture supervisor prefill from step 3
        @SuppressWarnings("unchecked")
        var supervisorCombo = (ComboBox<User>) $(ComboBox.class).first();
        User prefilledSupervisor = supervisorCombo.getValue();
        assertNotNull("Default supervisor should be pre-selected",
                prefilledSupervisor);

        // THEN: Both address and supervisor were prefilled from the same last
        // purchase
        assertNotNull(prefilledStreet);
        assertFalse(prefilledStreet.isEmpty());
        assertNotNull(prefilledSupervisor);
    }

    private NumberField getNumberField(Grid<ProductDto> productGrid) {
        return $((HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
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

        // Decompose details HTML and verify ARIA + purchase line rendering
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
