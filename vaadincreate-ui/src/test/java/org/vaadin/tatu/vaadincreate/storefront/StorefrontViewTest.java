package org.vaadin.tatu.vaadincreate.storefront;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.common.NumberField;

import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
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
        var productGrid = $(Grid.class).first();
        assertNotNull("Product grid should be present", productGrid);

        // AND: Next button should be present
        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption()
                        .contains("Next"))
                .findFirst().orElse(null);
        assertNotNull("Next button should be present", nextButton);

        // AND: Previous button should be disabled on first step
        var prevButton = $(Button.class).stream()
                .filter(b -> b.getCaption() != null
                        && b.getCaption().contains(
                                "Previous"))
                .findFirst().orElse(null);
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
        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption()
                        .contains("Next"))
                .findFirst().orElse(null);
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
        var productGrid = (Grid<ProductDto>) $(Grid.class).first();
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
        var productGrid = (Grid<ProductDto>) $(Grid.class).first();
        test(productGrid).clickToSelect(0);

        var numberField = $(
                (HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
        test(numberField).setValue(2);

        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption()
                        .contains("Next"))
                .findFirst().get();
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
        var productGrid = (Grid<ProductDto>) $(Grid.class).first();
        test(productGrid).clickToSelect(0);

        var numberField = $(
                (HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
        test(numberField).setValue(1);

        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption()
                        .contains("Next"))
                .findFirst().get();
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
        var productGrid = (Grid<ProductDto>) $(Grid.class).first();
        test(productGrid).clickToSelect(0);

        var numberField = $(
                (HorizontalLayout) test(productGrid).cell(3, 0),
                NumberField.class).first();
        test(numberField).setValue(2);

        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption()
                        .contains("Next"))
                .findFirst().get();
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
        test(nextButton).click();

        // Verify serialization in step 4
        SerializationDebugUtil.assertSerializable(view);

        // Submit
        var submitButton = $(Button.class).stream()
                .filter(b -> b.getCaption() != null
                        && b.getCaption().contains(
                                "Submit"))
                .findFirst().get();

        // WHEN: User submits the purchase
        test(submitButton).click();

        // THEN: Success notification should be shown
        var hasSuccessNotification = $(com.vaadin.ui.Notification.class)
                .stream().filter(n -> n.getCaption() != null)
                .anyMatch(n -> n.getCaption()
                        .contains("created"));
        assertTrue("Success notification should contain 'created'",
                hasSuccessNotification);
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
        assertEquals(PurchaseStatus.COMPLETED,
                test(historyGrid).cell(statusColumnIndex, 0));

        // AND: Verify serialization
        SerializationDebugUtil.assertSerializable(view);
    }
}
