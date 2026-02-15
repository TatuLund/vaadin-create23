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
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

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
        wizard = new PurchaseWizard();

        // Create purchase history grid on the right
        historyGrid = new PurchaseHistoryGrid(presenter, currentUser);
        historyGrid.addStyleName(VaadinCreateTheme.STOREFRONTVIEW_HISTORY);

        addComponents(wizard, historyGrid);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(VIEW_NAME);
        logger.info("Entered StorefrontView");

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
                .filter(p -> p.getStatus() == PurchaseStatus.COMPLETED).count();
        long rejectedCount = recentlyDecided.stream()
                .filter(p -> p.getStatus() == PurchaseStatus.REJECTED).count();
        long cancelledCount = recentlyDecided.stream()
                .filter(p -> p.getStatus() == PurchaseStatus.CANCELLED).count();

        StringBuilder message = new StringBuilder();
        message.append("Purchase Status Updates:\n\n");

        if (completedCount > 0) {
            message.append(completedCount).append(" purchase")
                    .append(completedCount > 1 ? "s" : "")
                    .append(" completed\n");
        }
        if (rejectedCount > 0) {
            message.append(rejectedCount).append(" purchase")
                    .append(rejectedCount > 1 ? "s" : "").append(" rejected\n");
        }
        if (cancelledCount > 0) {
            message.append(cancelledCount).append(" purchase")
                    .append(cancelledCount > 1 ? "s" : "")
                    .append(" cancelled\n");
        }

        message.append("\nCheck your purchase history for details.");

        Notification.show("Status Updates", message.toString(),
                Type.TRAY_NOTIFICATION);

        logger.info(
                "Showed status notification for {} decided purchases (completed: {}, rejected: {}, cancelled: {})",
                recentlyDecided.size(), completedCount, rejectedCount,
                cancelledCount);
    }
}
