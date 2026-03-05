package org.vaadin.tatu.vaadincreate.purchases;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
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
    /** Id of the purge confirmation window. */
    public static final String PURGE_WINDOW_ID = "purge-confirm-window";
    /** Id of the OK/confirm button inside the purge confirmation window. */
    public static final String PURGE_CONFIRM_OK_ID = "purge-confirm-ok";
    /** Id of the cancel button inside the purge confirmation window. */
    public static final String PURGE_CONFIRM_CANCEL_ID = "purge-confirm-cancel";

    /** Retention period in months (matching the PRD requirement). */
    static final int RETENTION_MONTHS = 24;

    public static final String VIEW_NAME = I18n.Storefront.PURCHASE_HISTORY;

    private final PurchaseHistoryGrid historyGrid;
    private final PurchaseHistoryPresenter presenter;
    private final Button purgeButton;

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
        long oldCount = presenter.countPurchasesOlderThan(cutoff);
        if (oldCount > 0) {
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
        var window = new Window(
                getTranslation(I18n.Storefront.PURGE_CONFIRM_CAPTION));
        window.setId(PURGE_WINDOW_ID);
        window.setModal(true);
        window.setClosable(true);
        window.setResizable(false);
        window.setWidth("450px");

        var message = new Label(
                getTranslation(I18n.Storefront.PURGE_CONFIRM_MESSAGE));

        var confirmButton = new Button(getTranslation(I18n.Storefront.PURGE),
                VaadinIcons.TRASH);
        confirmButton.addClickListener(e -> {
            window.close();
            executePurge();
        });
        confirmButton.setId(PURGE_CONFIRM_OK_ID);
        confirmButton.addStyleName(ValoTheme.BUTTON_DANGER);
        confirmButton.setDisableOnClick(true);

        var cancelButton = new Button(getTranslation(I18n.CANCEL),
                e -> window.close());
        cancelButton.setId(PURGE_CONFIRM_CANCEL_ID);

        var buttonBar = new HorizontalLayout(confirmButton, cancelButton);
        buttonBar.setSpacing(true);

        var content = new VerticalLayout(message, buttonBar);
        content.setComponentAlignment(buttonBar, Alignment.BOTTOM_RIGHT);
        content.setMargin(true);
        content.setSpacing(true);

        window.setContent(content);
        getUI().addWindow(window);
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
