package org.vaadin.tatu.vaadincreate.storefront;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.events.PurchaseStatusChangedEvent;
import org.vaadin.tatu.vaadincreate.common.NumberField;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

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
 * UI unit tests for StorefrontView and PurchaseWizard.
 */
public class StorefrontViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private StorefrontView view;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        // Login as CUSTOMER to access storefront
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
    public void should_ShowWizardSteps_When_NavigatingThroughWizard() {
        // GIVEN: Storefront view is displayed
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // THEN: First step (product selection) should be shown using ID
        var stepTitle = $(Label.class).id("wizard-step-title");
        assertNotNull("Step title should be present", stepTitle);

        // AND: Product grid should be present
        var productGrid = $(Grid.class).id("purchase-grid");
        assertNotNull("Product grid should be present", productGrid);

        // AND: Next button should be present
        var nextButton = $(Button.class).caption("Next").first();
        assertNotNull("Next button should be present", nextButton);

        // AND: Previous button should be disabled on first step
        var prevButton = $(Button.class).caption("Previous").first();
        assertNotNull("Previous button should be present", prevButton);
        assertFalse("Previous button should be disabled on first step",
                prevButton.isEnabled());

        // AND: Verify serialization
        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    public void should_RequireCartItems_When_ProceedingFromStep1() {
        // GIVEN: Storefront view is displayed with empty cart
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // WHEN: User tries to proceed without adding items
        var nextButton = $(Button.class).caption("Next").first();
        assertNotNull(nextButton);
        test(nextButton).click();

        // THEN: Warning notification should be shown
        assertNotification(
                "Your cart is empty. Please add items before proceeding.");
    }

    @Test
    public void should_SelectProductAndSetQuantity_When_UserInteracts() {
        // GIVEN: Storefront view with product grid
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        @SuppressWarnings("unchecked")
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        assertNotNull(productGrid);

        // WHEN: User selects first product by clicking checkbox column
        test(productGrid).clickToSelect(0);

        // Verify serialization after interaction
        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    public void should_ValidateAddress_When_ProceedingFromStep2() {
        // GIVEN: User navigates to step 2
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        @SuppressWarnings("unchecked")
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);

        var numberField = $(
                (HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
        test(numberField).setValue(2);

        var nextButton = $(Button.class).caption("Next").first();
        test(nextButton).click();

        // Verify serialization in step 2
        SerializationDebugUtil.assertSerializable(view);

        // WHEN: User tries to proceed with empty address fields
        test(nextButton).click();

        // THEN: Validation message should be shown
        assertNotification("Please fill all required fields");
    }

    @Test
    public void should_RequireSupervisor_When_ProceedingFromStep3() {
        // GIVEN: User navigates to step 3
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        @SuppressWarnings("unchecked")
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);

        var numberField = $(
                (HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
        test(numberField).setValue(1);

        var nextButton = $(Button.class).caption("Next").first();
        test(nextButton).click();

        // Fill address
        test($(TextField.class).caption("Street").first())
                .setValue("123 Main St");
        test($(TextField.class).caption("Postal Code").first())
                .setValue("12345");
        test($(TextField.class).caption("City").first())
                .setValue("TestCity");
        test($(TextField.class).caption("Country").first())
                .setValue("TestCountry");

        test(nextButton).click();

        // Verify serialization in step 3
        SerializationDebugUtil.assertSerializable(view);

        // WHEN: User tries to proceed without selecting supervisor
        test(nextButton).click();

        // THEN: Validation message should be shown
        assertNotification("Please select a supervisor");
    }

    @Test
    public void should_CreatePurchase_When_WizardCompleted() {
        // GIVEN: User completes all wizard steps
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        @SuppressWarnings("unchecked")
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);

        var numberField = $(
                (HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
        test(numberField).setValue(2);

        var nextButton = $(Button.class).caption("Next").first();
        test(nextButton).click();

        // Fill address
        test($(TextField.class).caption("Street").first())
                .setValue("123 Main St");
        test($(TextField.class).caption("Postal Code").first())
                .setValue("12345");
        test($(TextField.class).caption("City").first())
                .setValue("TestCity");
        test($(TextField.class).caption("Country").first())
                .setValue("TestCountry");
        test(nextButton).click();

        // Select supervisor
        @SuppressWarnings("unchecked")
        var supervisorCombo = (ComboBox<Object>) $(ComboBox.class)
                .first();
        assertNotNull("Supervisor combobox should be present",
                supervisorCombo);
        var supervisors = supervisorCombo.getDataCommunicator()
                .fetchItemsWithRange(0, 1);
        test(supervisorCombo).clickItem(supervisors.get(0));

        long assistiveBeforeReview = $(Notification.class).stream()
                .filter(n -> n.getPosition() == Position.ASSISTIVE).count();
        test(nextButton).click();

        // THEN: Entering review step shows an assistive notification
        long assistiveAfterReview = $(Notification.class).stream()
                .filter(n -> n.getPosition() == Position.ASSISTIVE).count();
        assertTrue(
                "Expected an assistive notification when entering review step",
                assistiveAfterReview > assistiveBeforeReview);
        assertTrue(
                "Expected review assistive notification to contain 'Order Summary'",
                $(Notification.class).stream()
                        .anyMatch(n -> n.getPosition() == Position.ASSISTIVE
                                && n.getCaption() != null
                                && n.getCaption().contains("Order Summary")));

        // Verify serialization in step 4
        SerializationDebugUtil.assertSerializable(view);

        // Submit
        var submitButton = $(Button.class).caption("Submit").first();

        // WHEN: User submits the purchase
        test(submitButton).click();

        // THEN: Success notification should be shown
        var hasSuccessNotification = $(Notification.class)
                .stream().filter(n -> n.getCaption() != null)
                .anyMatch(n -> n.getCaption()
                        .contains("created"));
        assertTrue("Success notification should contain 'created'",
                hasSuccessNotification);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void clicking_sorting_product_grid_by_price_will_sort_ascending_second_click_descending() {
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");

        int size = test(productGrid).size();

        // WHEN: Clicking price column sorting toggle
        test(productGrid).toggleColumnSorting(2);

        // THEN: Grid is sorted by price in ascending order
        for (int i = 1; i < size; i++) {
            var result = test(productGrid).item(i - 1).getPrice()
                    .compareTo(test(productGrid).item(i).getPrice());
            assertTrue(result <= 0);
        }

        // WHEN: Clicking price column sorting toggle again
        test(productGrid).toggleColumnSorting(2);

        // THEN: Grid is sorted by price in descending order
        for (int i = 1; i < size; i++) {
            var result = test(productGrid).item(i - 1).getPrice()
                    .compareTo(test(productGrid).item(i).getPrice());
            assertTrue(result >= 0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void clicking_sorting_product_grid_by_name_will_sort_ascending_second_click_descending() {
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");

        int size = test(productGrid).size();

        // WHEN: Clicking name column sorting toggle
        test(productGrid).toggleColumnSorting(0);

        // THEN: Grid is sorted by name in alphabetically ascending order
        for (int i = 1; i < size; i++) {
            var result = test(productGrid).item(i - 1).getProductName()
                    .compareToIgnoreCase(
                            test(productGrid).item(i).getProductName());
            assertTrue(result <= 0);
        }

        // WHEN: Clicking name column sorting toggle again
        test(productGrid).toggleColumnSorting(0);

        // THEN: Grid is sorted by name in alphabetically descending order
        for (int i = 1; i < size; i++) {
            var result = test(productGrid).item(i - 1).getProductName()
                    .compareToIgnoreCase(
                            test(productGrid).item(i).getProductName());
            assertTrue(result >= 0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void clicking_sorting_product_grid_by_stock_will_sort_ascending_second_click_descending() {
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");

        int size = test(productGrid).size();

        // WHEN: Clicking stock column sorting toggle
        test(productGrid).toggleColumnSorting(1);

        // THEN: Grid is sorted by stock in ascending order
        for (int i = 1; i < size; i++) {
            var result = Integer.compare(test(productGrid).item(i - 1)
                    .getStockCount(),
                    test(productGrid).item(i)
                            .getStockCount());
            assertTrue(result <= 0);
        }

        // WHEN: Clicking stock column sorting toggle again
        test(productGrid).toggleColumnSorting(1);

        // THEN: Grid is sorted by stock in descending order
        for (int i = 1; i < size; i++) {
            var result = Integer.compare(test(productGrid).item(i - 1)
                    .getStockCount(),
                    test(productGrid).item(i)
                            .getStockCount());
            assertTrue(result >= 0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_ShowPurchaseHistoryGrid_When_ViewIsDisplayed() {
        // GIVEN: User is logged in as CUSTOMER
        // WHEN: Navigate to storefront view
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // THEN: Purchase history grid should be present
        var historyGrid = $(Grid.class).id("purchase-history-grid");
        assertNotNull("Purchase history grid should be present",
                historyGrid);
        assertTrue("Purchase history grid should have at least one row",
                historyGrid.getDataCommunicator()
                        .getDataProviderSize() > 0);
        int statusColumnIndex = -1;
        for (int i = 0; i < historyGrid.getColumns().size(); i++) {
            @SuppressWarnings("rawtypes")
            Grid.Column column = (Grid.Column) historyGrid.getColumns().get(i);
            if ("status".equals(column.getId())) {
                statusColumnIndex = i;
                break;
            }
        }
        assertTrue("Status column not found", statusColumnIndex >= 0);
        boolean hasCompletedPurchase = false;
        for (int row = 0; row < historyGrid.getDataCommunicator()
                .getDataProviderSize(); row++) {
            if (PurchaseStatus.COMPLETED
                    .equals(test(historyGrid).cell(statusColumnIndex, row))) {
                hasCompletedPurchase = true;
                break;
            }
        }
        assertTrue("At least one purchase should have COMPLETED status",
                hasCompletedPurchase);

        // AND: Verify serialization
        SerializationDebugUtil.assertSerializable(view);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void entering_quantities_sorts_products_descending_by_order_quantity() {
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        assertTrue("Expected at least 2 products in purchase grid",
                test(productGrid).size() >= 2);

        var firstName = test(productGrid).item(0).getProductName();
        var secondName = test(productGrid).item(1).getProductName();

        // Select two items so that quantity inputs are rendered
        test(productGrid).clickToSelect(0);
        test(productGrid).clickToSelect(1);

        // Set the second product quantity higher -> it should bubble to the top
        int secondRowIndex = -1;
        for (int i = 0; i < test(productGrid).size(); i++) {
            if (secondName.equals(test(productGrid).item(i).getProductName())) {
                secondRowIndex = i;
                break;
            }
        }
        assertTrue("Second product row not found", secondRowIndex >= 0);
        var secondQtyField = $((HorizontalLayout) test(productGrid).cell(3,
                secondRowIndex), NumberField.class).first();
        test(secondQtyField).setValue(5);

        // Set the first product quantity lower
        int firstRowIndex = -1;
        for (int i = 0; i < test(productGrid).size(); i++) {
            if (firstName.equals(test(productGrid).item(i).getProductName())) {
                firstRowIndex = i;
                break;
            }
        }
        assertTrue("First product row not found", firstRowIndex >= 0);
        var firstQtyField = $((HorizontalLayout) test(productGrid).cell(3,
                firstRowIndex), NumberField.class).first();
        test(firstQtyField).setValue(1);

        // THEN: Product with higher order quantity is shown first
        assertEquals(secondName, test(productGrid).item(0).getProductName());
        assertEquals(Integer.valueOf(5), test(productGrid).item(0)
                .getOrderQuantity());
        assertEquals(firstName, test(productGrid).item(1).getProductName());
        assertEquals(Integer.valueOf(1), test(productGrid).item(1)
                .getOrderQuantity());
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
        Purchase pendingPurchase = null;
        int row = -1;
        do {
            row++;
            pendingPurchase = test(historyGrid).item(row);
        } while (pendingPurchase != null
                && !PurchaseStatus.PENDING.equals(pendingPurchase.getStatus())
                && row < initialSize);
        assertEquals("PENDING", test(historyGrid).cell(5, row).toString());

        // This simulates superviser approving the purchase in another session,
        // which triggers a PurchaseStatusChangedEvent that the grid listens to
        // and should refresh the item
        PurchaseService.get().approve(pendingPurchase.getId(),
                pendingPurchase.getApprover(), "Looks good");
        EventBus.get()
                .post(new PurchaseStatusChangedEvent(pendingPurchase.getId()));

        final int finalRow = row;
        waitWhile(Grid.class,
                grid -> test(historyGrid).cell(5, finalRow).toString()
                        .equals("PENDING"),
                1);

        // THEN: Grid size is unchaged
        int sizeAfterEvent = test(historyGrid).size();
        assertEquals(
                "Grid size should not increase after PurchaseStatusChangedEvent",
                initialSize, sizeAfterEvent);
        // AND: The updated purchase should have the new status
        assertNotEquals("PENDING", test(historyGrid).cell(5, row).toString());
        // AND: A notification about the status change should be shown
        assertNotNull($(Notification.class).last());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void purchase_history_grid_refreshes_when_wizard_is_submitted() {
        // GIVEN: Storefront view is displayed
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);
        var historyGrid = (Grid<?>) $(Grid.class).id("purchase-history-grid");
        assertNotNull("Purchase history grid should be present", historyGrid);
        int initialSize = test(historyGrid).size();

        // WHEN: User completes and submits the purchase wizard
        var productGrid = (Grid<ProductDto>) $(Grid.class).id("purchase-grid");
        test(productGrid).clickToSelect(0);
        var numberField = $(
                (HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
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
        test(supervisorCombo).clickItem(
                supervisorCombo.getDataCommunicator().fetchItemsWithRange(0, 1)
                        .get(0));
        test(nextButton).click();
        var submitButton = $(Button.class).caption("Submit").first();
        test(submitButton).click();

        // THEN: Success notification is shown and history grid has a new entry
        assertTrue("Success notification should be shown",
                $(Notification.class).stream()
                        .anyMatch(n -> n.getCaption() != null
                                && n.getCaption().contains("created")));
        int sizeAfterSubmit = test(historyGrid).size();
        assertTrue(
                "Purchase history grid should have more entries after a new purchase is created",
                sizeAfterSubmit > initialSize);
        assertEquals("PENDING", test(historyGrid).cell(5, 0).toString());
    }
}
