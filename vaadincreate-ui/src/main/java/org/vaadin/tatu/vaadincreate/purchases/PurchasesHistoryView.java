package org.vaadin.tatu.vaadincreate.purchases;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.components.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * History tab for PurchasesView. Displays all purchases for admin users and
 * provides a GDPR-inspired purge action for purchases older than 24 months.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchasesHistoryView extends VerticalLayout
        implements TabView, HasI18N {

    /** Id of the Purge button, used by tests to locate it. */
    public static final String PURGE_BUTTON_ID = "purge-button";

    /** Retention period in months (matching the PRD requirement). */
    static final int RETENTION_MONTHS = 24;

    public static final String VIEW_NAME = I18n.Storefront.PURCHASE_HISTORY;

    private final PurchaseHistoryGrid historyGrid;
    private final PurchaseHistoryPresenter presenter;
    private final Button purgeButton;
    private long purgeCount;

    public PurchasesHistoryView() {
        setSizeFull();
        setMargin(false);
        presenter = new PurchaseHistoryPresenter();
        historyGrid = new PurchaseHistoryGrid(presenter,
                PurchaseHistoryMode.ALL,
                Utils.getCurrentUserOrThrow());
        purgeButton = buildPurgeButton();

        var toolbar = new HorizontalLayout(purgeButton);
        toolbar.setComponentAlignment(purgeButton, Alignment.MIDDLE_RIGHT);
        toolbar.setWidth("100%");

        addComponent(toolbar);
        addComponent(historyGrid);
        setExpandRatio(historyGrid, 1);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(event);
        checkRetentionPolicy();
    }

    @Override
    public String getTabName() {
        return VIEW_NAME;
    }

    /**
     * Queries the presenter for purgeable purchases and updates the view
     * accordingly: shows a warning notification and enables the Purge button
     * when old purchases exist; hides the button and clears row highlighting
     * when none remain.
     */
    void checkRetentionPolicy() {
        Instant cutoff = retentionCutoff();
        purgeCount = presenter.countPurchasesOlderThan(cutoff);
        if (purgeCount > 0) {
            Notification.show(
                    getTranslation(
                            I18n.Storefront.PURGE_OLD_PURCHASES_NOTIFICATION),
                    Type.WARNING_MESSAGE);
            purgeButton.setVisible(true);
            historyGrid.setOldPurchaseHighlight(cutoff);
        } else {
            purgeButton.setVisible(false);
            historyGrid.setOldPurchaseHighlight(null);
        }
    }

    private Button buildPurgeButton() {
        var button = new Button(getTranslation(I18n.Storefront.PURGE),
                VaadinIcons.TRASH);
        button.setId(PURGE_BUTTON_ID);
        button.addStyleName(ValoTheme.BUTTON_DANGER);
        button.setVisible(false);
        button.addClickListener(e -> openPurgeConfirmDialog());
        return button;
    }

    private void openPurgeConfirmDialog() {
        var dialog = new ConfirmDialog(
                getTranslation(I18n.Storefront.PURGE_CONFIRM_CAPTION),
                getTranslation(I18n.Storefront.PURGE_CONFIRM_MESSAGE,
                        purgeCount),
                ConfirmDialog.Type.ALERT);
        dialog.setConfirmText(getTranslation(I18n.Storefront.PURGE));
        dialog.setCancelText(getTranslation(I18n.CANCEL));
        dialog.addConfirmedListener(e -> executePurge());
        dialog.open();
    }

    private void executePurge() {
        Instant cutoff = retentionCutoff();
        long purged = presenter.purgePurchases(cutoff);
        Notification.show(
                getTranslation(I18n.Storefront.PURGE_SUCCESS, purged),
                Type.HUMANIZED_MESSAGE);
        historyGrid.refresh();
        checkRetentionPolicy();
    }

    private static Instant retentionCutoff() {
        return ZonedDateTime.now(ZoneOffset.UTC).minusMonths(RETENTION_MONTHS)
                .toInstant();
    }
}
