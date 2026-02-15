package org.vaadin.tatu.vaadincreate.storefront;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.data.provider.CallbackDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.Grid;
import com.vaadin.ui.renderers.NumberRenderer;

/**
 * Grid component for displaying purchase history. Supports pagination via
 * callback data provider.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchaseHistoryGrid extends Grid<Purchase>
        implements HasI18N, HasAttributes<PurchaseHistoryGrid> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final StorefrontPresenter presenter;
    private final User currentUser;

    /**
     * Creates a new PurchaseHistoryGrid.
     * 
     * @param presenter
     *            the presenter for data access
     * @param currentUser
     *            the current user
     */
    public PurchaseHistoryGrid(StorefrontPresenter presenter,
            User currentUser) {
        this.presenter = Objects.requireNonNull(presenter,
                "Presenter must not be null");
        this.currentUser = Objects.requireNonNull(currentUser,
                "Current user must not be null");

        setId("purchase-history-grid");
        setSizeFull();
        setAccessibleNavigation(true);
        addStyleNames(VaadinCreateTheme.GRID_ROW_FOCUS);
        setRole(AriaRoles.REGION);
        setAriaLabel("Purchase History");

        configureColumns();
        configureDataProvider();
        configureDetailsGenerator();
    }

    private void configureColumns() {
        // Created At column
        addColumn(purchase -> {
            Instant createdAt = purchase.getCreatedAt();
            return createdAt != null ? DATE_FORMATTER.format(createdAt) : "";
        }).setCaption("Created").setId("created-at");

        // Status column
        addColumn(Purchase::getStatus).setCaption("Status").setId("status");

        // Total Amount column
        NumberFormat euroFormat = new DecimalFormat("#,##0.00 €");
        addColumn(Purchase::getTotalAmount,
                new NumberRenderer(euroFormat, Locale.getDefault()))
                .setCaption("Total").setId("total-amount");
    }

    private void configureDataProvider() {
        DataProvider<Purchase, Void> dataProvider = new CallbackDataProvider<>(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    return presenter
                            .fetchMyPurchases(currentUser, offset, limit)
                            .stream();
                }, query -> {
                    return (int) presenter.countMyPurchases(currentUser);
                });

        setDataProvider(dataProvider);
    }

    private void configureDetailsGenerator() {
        setDetailsGenerator(purchase -> {
            StringBuilder details = new StringBuilder();
            details.append("<div style='padding: 10px;'>");
            details.append("<strong>Purchase ID:</strong> ")
                    .append(purchase.getId() != null ? purchase.getId() : "N/A")
                    .append("<br>");

            User approver = purchase.getApprover();
            details.append("<strong>Approver:</strong> ")
                    .append(approver != null ? approver.getName() : "Pending")
                    .append("<br>");

            Instant decidedAt = purchase.getDecidedAt();
            details.append("<strong>Decided At:</strong> ")
                    .append(decidedAt != null ? DATE_FORMATTER.format(decidedAt)
                            : "N/A")
                    .append("<br>");

            String decisionReason = purchase.getDecisionReason();
            if (decisionReason != null && !decisionReason.isEmpty()) {
                details.append("<strong>Decision Reason:</strong> ")
                        .append(decisionReason).append("<br>");
            }

            details.append("</div>");
            return new com.vaadin.ui.Label(details.toString(),
                    com.vaadin.shared.ui.ContentMode.HTML);
        });
    }

    /**
     * Refreshes the grid data.
     */
    public void refresh() {
        getDataProvider().refreshAll();
    }
}
