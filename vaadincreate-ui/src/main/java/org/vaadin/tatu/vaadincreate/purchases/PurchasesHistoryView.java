package org.vaadin.tatu.vaadincreate.purchases;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService.PurchaseExportRow;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.components.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * History tab for PurchasesView. Displays all purchases for admin users and
 * provides a GDPR-inspired purge action for purchases older than
 * {@value #RETENTION_MONTHS} months.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchasesHistoryView extends VerticalLayout
        implements TabView {

    /** Id of the Purge button, used by tests to locate it. */
    public static final String PURGE_BUTTON_ID = "purge-button";
    public static final String FROM_DATE_ID = "export-from-date";
    public static final String TO_DATE_ID = "export-to-date";
    public static final String EXPORT_BUTTON_ID = "export-button";

    /** Retention period in months (matching the PRD requirement). */
    static final int RETENTION_MONTHS = 24;

    public static final String VIEW_NAME = I18n.Storefront.PURCHASE_HISTORY;

    private final PurchaseHistoryGrid historyGrid;
    private final PurchaseHistoryPresenter presenter;
    private final DateField fromDate;
    private final DateField toDate;
    private final Button exportButton;
    private final Button purgeButton;
    private final PurchaseHistoryCsvExporter csvExporter;
    @Nullable
    private UI ui;
    @Nullable
    private transient CompletableFuture<Void> runningExport;
    private long purgeCount;

    public PurchasesHistoryView() {
        setSizeFull();
        setMargin(false);
        addStyleName(VaadinCreateTheme.PURCHASEHISTORYVIEW);
        presenter = new PurchaseHistoryPresenter();
        historyGrid = new PurchaseHistoryGrid(presenter,
                PurchaseHistoryMode.ALL, Utils.getCurrentUserOrThrow());
        fromDate = buildDateField(I18n.Storefront.FROM, FROM_DATE_ID);
        toDate = buildDateField(I18n.Storefront.TO, TO_DATE_ID);
        exportButton = buildExportButton();
        purgeButton = buildPurgeButton();
        csvExporter = new PurchaseHistoryCsvExporter();

        var toolbar = new HorizontalLayout(fromDate, toDate, exportButton,
                purgeButton);
        toolbar.addStyleName(VaadinCreateTheme.PURCHASEHISTORYVIEW_TOOLBAR);
        toolbar.setComponentAlignment(fromDate, Alignment.MIDDLE_RIGHT);
        toolbar.setComponentAlignment(toDate, Alignment.MIDDLE_RIGHT);
        toolbar.setComponentAlignment(exportButton, Alignment.MIDDLE_RIGHT);
        toolbar.setComponentAlignment(purgeButton, Alignment.MIDDLE_RIGHT);
        toolbar.setExpandRatio(fromDate, 1);
        toolbar.setWidth("100%");
        toolbar.setSpacing(true);

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

    @Override
    public void attach() {
        super.attach();
        ui = getUI();
    }

    @Override
    public void detach() {
        super.detach();
        if (runningExport != null) {
            runningExport.cancel(false);
            runningExport = null;
        }
        ui = null;
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
            Notification.show(getTranslation(
                    I18n.Storefront.PURGE_OLD_PURCHASES_NOTIFICATION,
                    RETENTION_MONTHS), Type.WARNING_MESSAGE);
            purgeButton.setEnabled(true);
            historyGrid.setOldPurchaseHighlight(cutoff);
            historyGrid.scrollToFromBottom((int) purgeCount);
        } else {
            purgeButton.setEnabled(false);
            historyGrid.setOldPurchaseHighlight(null);
        }
    }

    private DateField buildDateField(String translationKey, String id) {
        var field = new DateField(getTranslation(translationKey));
        field.setId(id);
        field.setRequiredIndicatorVisible(true);
        field.setRangeEnd(LocalDate.now());
        field.setRangeStart(LocalDate.now().minusMonths(24));
        field.addValueChangeListener(
                e -> onDateValueChanged(e.isUserOriginated(),
                        e.getSource() == toDate));
        return field;
    }

    private Button buildExportButton() {
        var button = new Button(getTranslation(I18n.Storefront.EXPORT),
                VaadinIcons.DOWNLOAD);
        button.setId(EXPORT_BUTTON_ID);
        button.setEnabled(false);
        button.setDisableOnClick(true);
        button.addClickListener(e -> startExport());
        return button;
    }

    private void onDateValueChanged(boolean userOriginated,
            boolean isToFieldChanged) {
        validateDateInputs();
        if (isDateRangeValid()) {
            var fromInstant = fromDate.getValue()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
            var toExclusive = toDate.getValue().plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant();
            historyGrid.setSelectedRangeHighlight(fromInstant, toExclusive);
            if (userOriginated && isToFieldChanged) {
                var index = presenter.resolveFirstMatchingRowIndex(
                        toDate.getValue());
                if (index != null) {
                    historyGrid.scrollToIndex(index);
                }
            }
        } else {
            historyGrid.setSelectedRangeHighlight(null, null);
        }
    }

    private void validateDateInputs() {
        toDate.setComponentError(null);
        if (fromDate.getValue() == null || toDate.getValue() == null) {
            exportButton.setEnabled(false);
            return;
        }
        if (toDate.getValue().isBefore(fromDate.getValue())) {
            toDate.setComponentError(new UserError(
                    getTranslation(I18n.Storefront.EXPORT_INVALID_RANGE)));
            exportButton.setEnabled(false);
            return;
        }
        if (toDate.getValue().isAfter(fromDate.getValue().plusMonths(3))) {
            toDate.setComponentError(new UserError(
                    getTranslation(I18n.Storefront.EXPORT_MAX_THREE_MONTHS)));
            exportButton.setEnabled(false);
            return;
        }
        exportButton.setEnabled(true);
    }

    private boolean isDateRangeValid() {
        return fromDate.getValue() != null && toDate.getValue() != null
                && toDate.getComponentError() == null;
    }

    private void startExport() {
        if (!isDateRangeValid()) {
            Notification.show(
                    getTranslation(I18n.Storefront.EXPORT_MISSING_DATES),
                    Type.WARNING_MESSAGE);
            resetExportButtonState();
            return;
        }
        if (runningExport != null && !runningExport.isDone()) {
            return;
        }
        exportButton.setIcon(VaadinIcons.SPINNER);
        Notification.show(getTranslation(I18n.Storefront.EXPORT_STARTED),
                Type.TRAY_NOTIFICATION);
        var from = fromDate.getValue();
        var to = toDate.getValue();
        runningExport = presenter.startExport(from, to,
                rows -> Utils.access(ui, () -> onExportReady(from, to, rows)),
                throwable -> Utils.access(ui, () -> {
                    Notification.show(
                            getTranslation(I18n.Storefront.EXPORT_FAILED),
                            Type.ERROR_MESSAGE);
                    resetExportButtonState();
                }));
    }

    private void onExportReady(LocalDate from, LocalDate to,
            List<PurchaseExportRow> rows) {
        var resource = csvExporter.createResource(from, to, rows, getLocale(),
                getTranslation(I18n.Storefront.EXPORT));
        var dialog = new PurchaseExportDownloadDialog(resource);
        dialog.addCloseListener(e -> resetExportButtonState());
        dialog.open();
        Notification.show(getTranslation(I18n.Storefront.EXPORT_READY),
                Type.HUMANIZED_MESSAGE);
    }

    private void resetExportButtonState() {
        exportButton.setIcon(VaadinIcons.DOWNLOAD);
        validateDateInputs();
        runningExport = null;
    }

    private Button buildPurgeButton() {
        var button = new Button(getTranslation(I18n.Storefront.PURGE),
                VaadinIcons.TRASH);
        button.setId(PURGE_BUTTON_ID);
        button.addStyleName(ValoTheme.BUTTON_DANGER);
        button.setEnabled(false);
        button.setDisableOnClick(true);
        button.addClickListener(e -> openPurgeConfirmDialog());
        return button;
    }

    private void openPurgeConfirmDialog() {
        var dialog = new ConfirmDialog(
                getTranslation(I18n.Storefront.PURGE_CONFIRM_CAPTION),
                getTranslation(I18n.Storefront.PURGE_CONFIRM_MESSAGE,
                        purgeCount, RETENTION_MONTHS),
                ConfirmDialog.Type.ALERT);
        dialog.setConfirmText(getTranslation(I18n.Storefront.PURGE));
        dialog.setCancelText(getTranslation(I18n.CANCEL));
        dialog.addConfirmedListener(e -> executePurge());
        dialog.addCancelledListener(e -> purgeButton.setEnabled(true));
        dialog.open();
    }

    private void executePurge() {
        Instant cutoff = retentionCutoff();
        long purged = presenter.purgePurchases(cutoff);
        Notification.show(getTranslation(I18n.Storefront.PURGE_SUCCESS, purged,
                RETENTION_MONTHS), Type.HUMANIZED_MESSAGE);
        historyGrid.refresh();
        checkRetentionPolicy();
    }

    private static Instant retentionCutoff() {
        return ZonedDateTime.now(ZoneOffset.UTC).minusMonths(RETENTION_MONTHS)
                .toInstant();
    }
}
