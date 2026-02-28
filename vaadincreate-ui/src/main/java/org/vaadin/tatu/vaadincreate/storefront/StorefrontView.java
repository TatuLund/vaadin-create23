package org.vaadin.tatu.vaadincreate.storefront;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateView;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.purchases.PurchaseHistoryGrid;
import org.vaadin.tatu.vaadincreate.purchases.PurchaseHistoryPresenter;
import org.vaadin.tatu.vaadincreate.util.Utils;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

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

    private final StorefrontPresenter presenter;
    private PurchaseWizard wizard;
    @Nullable
    private PurchaseHistoryGrid historyGrid;

    public StorefrontView() {
        setSizeFull();
        addStyleName(VaadinCreateTheme.STOREFRONTVIEW);

        presenter = new StorefrontPresenter();
        User currentUser = Utils.getCurrentUserOrThrow();

        // Create wizard on the left
        wizard = new PurchaseWizard(presenter);

        // Create purchase history grid on the right
        historyGrid = new PurchaseHistoryGrid(new PurchaseHistoryPresenter(),
                PurchaseHistoryMode.MY_PURCHASES, currentUser);

        var historyTitle = new Label(
                getTranslation(I18n.Storefront.PURCHASE_HISTORY));
        historyTitle.addStyleNames(ValoTheme.LABEL_H2, "v-margin-left",
                VaadinCreateTheme.STOREFRONTVIEW_HISTORY_LABEL);

        var historyLayout = new VerticalLayout(historyTitle, historyGrid);
        historyLayout.setMargin(false);
        historyLayout.setSpacing(false);
        historyLayout.setSizeFull();
        historyLayout.setExpandRatio(historyGrid, 1);
        historyLayout.addStyleNames(VaadinCreateTheme.STOREFRONTVIEW_HISTORY,
                ValoTheme.LAYOUT_WELL);

        historyGrid.setSizeFull();

        addComponents(wizard, historyLayout);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(VIEW_NAME);
        logger.info("Entered StorefrontView");

        // Load products via presenter
        var products = presenter.getOrderableProducts();
        wizard.setProducts(products);

        // Check for status changes and show notification
        checkForStatusChanges();
    }

    /**
     * Checks for recently decided purchases and shows a notification if any are
     * found.
     */
    private void checkForStatusChanges() {
        User currentUser = Utils.getCurrentUserOrThrow();
        Instant lastCheck = currentUser.getLastStatusCheck();

        // Determine the time window to check from
        Instant since;
        if (lastCheck != null) {
            since = lastCheck;
        } else {
            // If no previous check, look back 30 days
            since = Instant.now().minus(Duration.ofDays(30));
        }

        List<Purchase> recentlyDecided = presenter
                .findRecentlyDecidedPurchases(currentUser, since);

        if (!recentlyDecided.isEmpty()) {
            showStatusChangeNotification(recentlyDecided);
            // Update lastStatusCheck
            presenter.updateLastStatusCheck(currentUser, Instant.now());
        }
    }

    /**
     * Shows a notification dialog summarizing status changes.
     * 
     * @param recentlyDecided
     *            the list of recently decided purchases
     */
    private void showStatusChangeNotification(
            List<Purchase> recentlyDecided) {
        long completedCount = recentlyDecided.stream()
                .filter(purchase -> purchase
                        .getStatus() == PurchaseStatus.COMPLETED)
                .count();
        long rejectedCount = recentlyDecided.stream()
                .filter(purchase -> purchase
                        .getStatus() == PurchaseStatus.REJECTED)
                .count();
        long cancelledCount = recentlyDecided.stream()
                .filter(purchase -> purchase
                        .getStatus() == PurchaseStatus.CANCELLED)
                .count();

        String message = getMessage(completedCount, rejectedCount,
                cancelledCount);

        Notification.show(getTranslation(I18n.Storefront.STATUS_UPDATES_TITLE),
                message, Type.TRAY_NOTIFICATION);

        logger.info(
                "Showed status notification for {} decided purchases (completed: {}, rejected: {}, cancelled: {})",
                recentlyDecided.size(), completedCount, rejectedCount,
                cancelledCount);
    }

    private String getMessage(long completedCount, long rejectedCount,
            long cancelledCount) {
        StringBuilder message = new StringBuilder();
        message.append(getTranslation(I18n.Storefront.STATUS_UPDATES_HEADER))
                .append("\n\n");

        if (completedCount > 0) {
            message.append(getTranslation(
                    completedCount == 1
                            ? I18n.Storefront.STATUS_UPDATES_COMPLETED_ONE
                            : I18n.Storefront.STATUS_UPDATES_COMPLETED_MANY,
                    completedCount)).append("\n");
        }
        if (rejectedCount > 0) {
            message.append(getTranslation(
                    rejectedCount == 1
                            ? I18n.Storefront.STATUS_UPDATES_REJECTED_ONE
                            : I18n.Storefront.STATUS_UPDATES_REJECTED_MANY,
                    rejectedCount)).append("\n");
        }
        if (cancelledCount > 0) {
            message.append(getTranslation(
                    cancelledCount == 1
                            ? I18n.Storefront.STATUS_UPDATES_CANCELLED_ONE
                            : I18n.Storefront.STATUS_UPDATES_CANCELLED_MANY,
                    cancelledCount)).append("\n");
        }

        message.append("\n")
                .append(getTranslation(I18n.Storefront.STATUS_UPDATES_FOOTER));
        return message.toString();
    }
}
