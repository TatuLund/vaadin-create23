package org.vaadin.tatu.vaadincreate.storefront;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.provider.CallbackDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
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
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

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
        setWidth("90%");
        setHeight("100%");
        setAccessibleNavigation(true);
        addStyleNames(VaadinCreateTheme.GRID_ROW_FOCUS);
        setRole(AriaRoles.REGION);
        setAriaLabel(getTranslation(I18n.Storefront.PURCHASE_HISTORY));
        setSelectionMode(SelectionMode.NONE);

        configureColumns();
        configureDataProvider();
        configureDetailsGenerator();
        addItemClickListener(e -> {
            if (e.getItem() != null) {
                // Toggle details visibility on row click
                if (isDetailsVisible(e.getItem())) {
                    setDetailsVisible(e.getItem(), false);
                } else {
                    setDetailsVisible(e.getItem(), true);
                }
            }
        });
    }

    private void configureColumns() {
        // Created At column
        addColumn(purchase -> {
            Instant createdAt = purchase.getCreatedAt();
            return createdAt != null
                    ? DATE_FORMATTER.format(createdAt)
                    : "";
        }).setCaption(getTranslation(I18n.CREATED_AT)).setId("created-at");

        // Status column
        addColumn(Purchase::getStatus)
                .setCaption(getTranslation(I18n.STATUS))
                .setId("status");

        // Total Amount column
        NumberFormat euroFormat = new DecimalFormat("#,##0.00 €");
        addColumn(Purchase::getTotalAmount,
                new NumberRenderer(euroFormat))
                .setCaption(getTranslation(I18n.TOTAL))
                .setId("total-amount");
    }

    private void configureDataProvider() {
        DataProvider<Purchase, Void> dataProvider = new CallbackDataProvider<>(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    return presenter
                            .fetchMyPurchases(
                                    currentUser,
                                    offset,
                                    limit)
                            .stream();
                }, query -> {
                    return (int) presenter.countMyPurchases(
                            currentUser);
                });

        setDataProvider(dataProvider);
    }

    private void configureDetailsGenerator() {
        setDetailsGenerator(purchase -> {
            StringBuilder details = new StringBuilder();
            details.append("<div style='padding: 10px;'>");
            details.append("<strong>")
                    .append(getTranslation(I18n.Storefront.PURCHASE_ID))
                    .append(":</strong> ")
                    .append(purchase.getId() != null
                            ? purchase.getId()
                            : getTranslation(
                                    I18n.Storefront.NOT_AVAILABLE))
                    .append("<br>");

            User approver = purchase.getApprover();
            details.append("<strong>")
                    .append(getTranslation(I18n.Storefront.APPROVER))
                    .append(":</strong> ")
                    .append(approver != null
                            ? approver.getName()
                            : getTranslation(
                                    I18n.Storefront.PENDING))
                    .append("<br>");

            Instant decidedAt = purchase.getDecidedAt();
            details.append("<strong>")
                    .append(getTranslation(I18n.Storefront.DECIDED_AT))
                    .append(":</strong> ")
                    .append(decidedAt != null
                            ? DATE_FORMATTER.format(
                                    decidedAt)
                            : getTranslation(
                                    I18n.Storefront.NOT_AVAILABLE))
                    .append("<br>");

            String decisionReason = purchase.getDecisionReason();
            if (decisionReason != null
                    && !decisionReason.isEmpty()) {
                details.append("<strong>")
                        .append(getTranslation(
                                I18n.Storefront.DECISION_REASON))
                        .append(":</strong> ")
                        .append(decisionReason)
                        .append("<br>");
            }

            details.append("</div>");
            return new Label(Utils.sanitize(details.toString()),
                    ContentMode.HTML);
        });
    }

    /**
     * Refreshes the grid data.
     */
    public void refresh() {
        getDataProvider().refreshAll();
    }
}
