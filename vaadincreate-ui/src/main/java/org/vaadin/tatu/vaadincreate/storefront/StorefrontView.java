package org.vaadin.tatu.vaadincreate.storefront;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateView;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

/**
 * A view for employees (CUSTOMER role) to create purchase requests using a
 * wizard interface and view their purchase history.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@RolesPermitted({ Role.CUSTOMER })
public class StorefrontView extends CssLayout implements VaadinCreateView {

    private static final Logger logger = LoggerFactory
            .getLogger(StorefrontView.class);

    public static final String VIEW_NAME = "storefront";

    private PurchaseWizard wizard;
    @Nullable
    private Label historyPlaceholder;

    public StorefrontView() {
        setSizeFull();
        addStyleName(VaadinCreateTheme.STOREFRONTVIEW);

        // Create wizard on the left
        wizard = new PurchaseWizard();

        // Create placeholder for history on the right
        historyPlaceholder = new Label("My Purchases (coming in Step 3)");
        historyPlaceholder.addStyleName(VaadinCreateTheme.STOREFRONTVIEW_HISTORY);

        addComponents(wizard, historyPlaceholder);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(VIEW_NAME);
        logger.info("Entered StorefrontView");
    }
}
