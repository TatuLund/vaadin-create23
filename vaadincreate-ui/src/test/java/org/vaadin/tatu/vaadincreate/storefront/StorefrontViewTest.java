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

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
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
        login("Customer0", "customer0");
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
        assertAssistiveNotification("Storefront opened");
    }

    @Test
    public void should_ShowWizardSteps_When_NavigatingThroughWizard() {
        // GIVEN: Storefront view is displayed
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // THEN: First step (product selection) should be shown
        var stepTitle = $(Label.class).stream()
                .filter(l -> l.getStyleName().contains("v-label-h2"))
                .findFirst().orElse(null);
        assertNotNull("Step title should be present", stepTitle);

        // AND: Product grid should be present
        var productGrid = $(Grid.class).first();
        assertNotNull("Product grid should be present", productGrid);

        // AND: Next button should be present
        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption().contains("Next"))
                .findFirst().orElse(null);
        assertNotNull("Next button should be present", nextButton);

        // AND: Previous button should be disabled on first step
        var prevButton = $(Button.class).stream()
                .filter(b -> b.getCaption() != null
                        && b.getCaption().contains("Previous"))
                .findFirst().orElse(null);
        assertNotNull("Previous button should be present", prevButton);
        assertFalse("Previous button should be disabled on first step",
                prevButton.isEnabled());
    }

    @Test
    public void should_RequireCartItems_When_ProceedingFromStep1() {
        // GIVEN: Storefront view is displayed with empty cart
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // WHEN: User tries to proceed without adding items
        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption().contains("Next"))
                .findFirst().orElse(null);
        assertNotNull(nextButton);
        test(nextButton).click();

        // THEN: Warning notification should be shown
        assertNotification(
                "Your cart is empty. Please add items before proceeding.");
    }

    @Test
    public void should_AddToCart_When_ProductSelected() {
        // GIVEN: Storefront view with product grid
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);
        var productGrid = $(Grid.class).first();
        assertNotNull(productGrid);

        // WHEN: User selects a product
        test(productGrid).click(1, 0);

        // AND: Enters quantity
        var quantityField = $(TextField.class).stream()
                .filter(f -> f.getCaption() != null
                        && f.getCaption().equals("Quantity"))
                .findFirst().orElse(null);
        assertNotNull("Quantity field should be present", quantityField);
        test(quantityField).setValue("2");

        // AND: Clicks add to cart
        var addButton = $(Button.class).stream()
                .filter(b -> b.getCaption() != null
                        && b.getCaption().contains("Add to Cart"))
                .findFirst().orElse(null);
        assertNotNull("Add to cart button should be present", addButton);
        test(addButton).click();

        // THEN: Success notification should be shown
        assertTrue("Cart should have items",
                $(Label.class).stream()
                        .anyMatch(l -> l.getValue().contains("Cart Items")
                                && l.getValue().contains("1")));
    }

    @Test
    public void should_ValidateAddress_When_ProceedingFromStep2() {
        // GIVEN: User is on step 2 (address form)
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // Add item to cart first
        var productGrid = $(Grid.class).first();
        test(productGrid).click(1, 0);
        var addButton = $(Button.class).stream()
                .filter(b -> b.getCaption() != null
                        && b.getCaption().contains("Add to Cart"))
                .findFirst().get();
        test(addButton).click();

        // Navigate to step 2
        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption().contains("Next"))
                .findFirst().get();
        test(nextButton).click();

        // WHEN: User tries to proceed with empty address fields
        test(nextButton).click();

        // THEN: Validation message should be shown
        assertNotification("Please fill all required fields");
    }

    @Test
    public void should_RequireSupervisor_When_ProceedingFromStep3() {
        // GIVEN: User is on step 3 (supervisor selection)
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // Complete step 1
        var productGrid = $(Grid.class).first();
        test(productGrid).click(1, 0);
        var addButton = $(Button.class).stream()
                .filter(b -> b.getCaption() != null
                        && b.getCaption().contains("Add to Cart"))
                .findFirst().get();
        test(addButton).click();

        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption().contains("Next"))
                .findFirst().get();
        test(nextButton).click();

        // Complete step 2 with valid address
        var streetField = $(TextField.class).stream().filter(
                f -> f.getCaption() != null && f.getCaption().equals("Street"))
                .findFirst().get();
        test(streetField).setValue("123 Main St");

        var postalField = $(TextField.class).stream()
                .filter(f -> f.getCaption() != null
                        && f.getCaption().contains("Postal"))
                .findFirst().get();
        test(postalField).setValue("12345");

        var cityField = $(TextField.class).stream().filter(
                f -> f.getCaption() != null && f.getCaption().equals("City"))
                .findFirst().get();
        test(cityField).setValue("TestCity");

        var countryField = $(TextField.class).stream().filter(
                f -> f.getCaption() != null && f.getCaption().equals("Country"))
                .findFirst().get();
        test(countryField).setValue("TestCountry");

        test(nextButton).click();

        // WHEN: User tries to proceed without selecting supervisor
        test(nextButton).click();

        // THEN: Validation message should be shown
        assertNotification("Please select a supervisor");
    }

    @Test
    public void should_CreatePurchase_When_WizardCompleted() {
        // GIVEN: User completes all wizard steps
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        // Step 1: Add product to cart
        var productGrid = $(Grid.class).first();
        test(productGrid).click(1, 0);
        var addButton = $(Button.class).stream()
                .filter(b -> b.getCaption() != null
                        && b.getCaption().contains("Add to Cart"))
                .findFirst().get();
        test(addButton).click();

        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption().contains("Next"))
                .findFirst().get();
        test(nextButton).click();

        // Step 2: Fill address
        test($(TextField.class).stream()
                .filter(f -> f.getCaption() != null
                        && f.getCaption().equals("Street"))
                .findFirst().get()).setValue("123 Main St");
        test($(TextField.class).stream()
                .filter(f -> f.getCaption() != null
                        && f.getCaption().contains("Postal"))
                .findFirst().get()).setValue("12345");
        test($(TextField.class).stream()
                .filter(f -> f.getCaption() != null
                        && f.getCaption().equals("City"))
                .findFirst().get()).setValue("TestCity");
        test($(TextField.class).stream()
                .filter(f -> f.getCaption() != null
                        && f.getCaption().equals("Country"))
                .findFirst().get()).setValue("TestCountry");
        test(nextButton).click();

        // Step 3: Select supervisor
        var supervisorCombo = $(ComboBox.class).first();
        assertNotNull("Supervisor combobox should be present", supervisorCombo);
        // Select first supervisor
        var supervisors = supervisorCombo.getDataCommunicator()
                .fetchItemsWithRange(0, 1);
        if (supervisors.size() > 0) {
            test(supervisorCombo).selectItem(supervisors.get(0));
            test(nextButton).click();

            // Step 4: Submit
            var submitButton = $(Button.class).stream()
                    .filter(b -> b.getCaption() != null
                            && b.getCaption().contains("Submit"))
                    .findFirst().get();

            // WHEN: User submits the purchase
            test(submitButton).click();

            // THEN: Success notification should be shown
            // The notification caption may be null, so we check safely
            var hasSuccessNotification = $(com.vaadin.ui.Notification.class)
                    .stream().filter(n -> n.getCaption() != null)
                    .anyMatch(n -> n.getCaption().contains("created"));
            assertTrue("Success notification should contain 'created'",
                    hasSuccessNotification);
        }
    }
}
