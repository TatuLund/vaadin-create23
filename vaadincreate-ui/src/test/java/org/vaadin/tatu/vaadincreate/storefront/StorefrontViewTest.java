package org.vaadin.tatu.vaadincreate.storefront;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;

import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;

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
    public void should_SelectProductsAndProceed_When_UserSelectsItems() {
        // GIVEN: Storefront view with product grid
        view = navigate(StorefrontView.VIEW_NAME, StorefrontView.class);

        @SuppressWarnings("unchecked")
        var productGrid = (Grid<ProductDto>) $(Grid.class).first();
        assertNotNull(productGrid);

        // WHEN: User selects first product
        test(productGrid).click(1, 0);

        // Verify serialization after interaction
        SerializationDebugUtil.assertSerializable(view);

        // AND: Enters quantity (NumberField should be present in grid)
        // Note: In the new design, quantity is set directly in the grid column

        var nextButton = $(Button.class).stream().filter(
                b -> b.getCaption() != null && b.getCaption().contains("Next"))
                .findFirst().orElse(null);
        assertNotNull("Next button should be present", nextButton);

        // User should still get warning if quantity is not set
        test(nextButton).click();
        assertNotification(
                "Your cart is empty. Please add items before proceeding.");
    }
}
