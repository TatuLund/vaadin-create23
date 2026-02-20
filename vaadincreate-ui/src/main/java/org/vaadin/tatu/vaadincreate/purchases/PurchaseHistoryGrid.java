package org.vaadin.tatu.vaadincreate.purchases;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.components.Html;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.provider.CallbackDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializableFunction;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.renderers.NumberRenderer;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Grid component for displaying purchase history. Supports pagination via
 * callback data provider.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchaseHistoryGrid extends Composite implements HasI18N {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final Grid<Purchase> grid = new Grid<>();
    private final PurchaseHistoryPresenter presenter;
    private final PurchaseHistoryMode mode;
    private final User currentUser;

    /**
     * Creates a new PurchaseHistoryGrid.
     *
     * @param presenter
     *            the presenter for data access
     * @param mode
     *            the history mode to use
     * @param currentUser
     *            the current user
     */
    public PurchaseHistoryGrid(PurchaseHistoryPresenter presenter,
            PurchaseHistoryMode mode, User currentUser) {
        this.presenter = Objects.requireNonNull(presenter,
                "Presenter must not be null");
        this.mode = Objects.requireNonNull(mode, "Mode must not be null");
        this.currentUser = Objects.requireNonNull(currentUser,
                "Current user must not be null");

        configureGrid();
        setCompositionRoot(grid);
    }

    private void configureGrid() {
        grid.setId(mode == PurchaseHistoryMode.PENDING_APPROVALS
                ? "purchase-approvals-grid"
                : "purchase-history-grid");
        if (mode == PurchaseHistoryMode.MY_PURCHASES) {
            grid.setWidth("90%");
        } else {
            grid.setWidth("100%");
        }
        grid.setHeight("100%");
        grid.setAccessibleNavigation(true);
        grid.addStyleNames(VaadinCreateTheme.GRID_ROW_FOCUS);
        AttributeExtension.of(grid).setAttribute(AriaAttributes.ROLE,
                AriaRoles.REGION);
        AttributeExtension.of(grid).setAttribute(AriaAttributes.LABEL,
                getTranslation(I18n.Storefront.PURCHASE_HISTORY));
        grid.setSelectionMode(Grid.SelectionMode.NONE);

        configureColumns();
        configureDataProvider();
        configureDetailsGenerator();
        setupDetailsToggle();
    }

    private void setupDetailsToggle() {
        grid.addItemClickListener(e -> {
            if (e.getItem() != null) {
                grid.setDetailsVisible(e.getItem(),
                        !grid.isDetailsVisible(e.getItem()));
                grid.getDataProvider().refreshItem(e.getItem());
            }
        });
    }

    private void configureColumns() {
        grid.addComponentColumn(ToggleButton::new).setWidth(50);

        var idColumn = grid.addColumn(p -> p.getId() != null ? p.getId() : "")
                .setCaption(getTranslation(I18n.Storefront.PURCHASE_ID))
                .setSortable(false).setId("purchase-id");

        var requesterColumn = grid.addColumn(
                p -> p.getRequester() != null ? p.getRequester().getName() : "")
                .setCaption(getTranslation(I18n.REQUESTER)).setSortable(false)
                .setId("requester");

        var approverColumn = grid
                .addColumn(
                        p -> p.getApprover() != null ? p.getApprover().getName()
                                : getTranslation(I18n.Storefront.PENDING))
                .setCaption(getTranslation(I18n.Storefront.APPROVER))
                .setSortable(false).setId("approver");

        grid.addColumn(purchase -> {
            Instant createdAt = purchase.getCreatedAt();
            return createdAt != null ? DATE_FORMATTER.format(createdAt) : "";
        }).setCaption(getTranslation(I18n.CREATED_AT)).setId("created-at")
                .setSortable(false);

        grid.addColumn(Purchase::getStatus)
                .setCaption(getTranslation(I18n.STATUS)).setSortable(false)
                .setId("status");

        var decidedAtColumn = grid.addColumn(purchase -> {
            Instant decidedAt = purchase.getDecidedAt();
            return decidedAt != null ? DATE_FORMATTER.format(decidedAt) : "";
        }).setCaption(getTranslation(I18n.Storefront.DECIDED_AT))
                .setId("decided-at").setSortable(false);

        NumberFormat euroFormat = new DecimalFormat("#,##0.00 €");
        grid.addColumn(Purchase::getTotalAmount, new NumberRenderer(euroFormat))
                .setCaption(getTranslation(I18n.TOTAL)).setId("total-amount")
                .setSortable(false);

        var decisionReasonColumn = grid.addColumn(purchase -> {
            var reason = purchase.getDecisionReason();
            return reason != null ? reason : "";
        }).setCaption(getTranslation(I18n.Storefront.DECISION_REASON))
                .setId("decision-reason").setSortable(false);

        if (mode == PurchaseHistoryMode.MY_PURCHASES) {
            idColumn.setHidden(true);
            requesterColumn.setHidden(true);
            approverColumn.setHidden(true);
            decidedAtColumn.setHidden(true);
            decisionReasonColumn.setHidden(true);
        }
    }

    private void configureDataProvider() {
        DataProvider<Purchase, Void> dataProvider = new CallbackDataProvider<>(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    return presenter
                            .fetchPurchases(mode, offset, limit, currentUser)
                            .stream();
                }, query -> {
                    return (int) presenter.countPurchases(mode, currentUser);
                });

        grid.setDataProvider(dataProvider);
    }

    private void configureDetailsGenerator() {
        grid.setDetailsGenerator(purchase -> {
            Html.Div htmlDiv;
            if (mode == PurchaseHistoryMode.MY_PURCHASES) {
                htmlDiv = detailsHtmlForMyPurchase(purchase);
            } else {
                htmlDiv = buildPurchaseLinesHtml(purchase);
            }
            htmlDiv.attr(AriaAttributes.LIVE, "assertive");
            htmlDiv.attr(AriaAttributes.ROLE, AriaRoles.ALERT);
            return new Label(htmlDiv.build(), ContentMode.HTML);
        });
    }

    private Html.Div detailsHtmlForMyPurchase(Purchase purchase) {
        var root = Html.div().style("padding: 10px;");

        var purchaseIdValue = purchase.getId() != null ? purchase.getId()
                : getTranslation(I18n.Storefront.NOT_AVAILABLE);
        root.add(
                Html.strong().text(getTranslation(I18n.Storefront.PURCHASE_ID)))
                .add(Html.span().text(": " + purchaseIdValue)).add(Html.br());

        User approver = purchase.getApprover();
        var approverValue = approver != null ? approver.getName()
                : getTranslation(I18n.Storefront.PENDING);
        root.add(Html.strong().text(getTranslation(I18n.Storefront.APPROVER)))
                .add(Html.span().text(": " + approverValue)).add(Html.br());

        Instant decidedAt = purchase.getDecidedAt();
        var decidedAtValue = decidedAt != null
                ? DATE_FORMATTER.format(decidedAt)
                : getTranslation(I18n.Storefront.NOT_AVAILABLE);
        root.add(Html.strong().text(getTranslation(I18n.Storefront.DECIDED_AT)))
                .add(Html.span().text(": " + decidedAtValue)).add(Html.br());

        String decisionReason = purchase.getDecisionReason();
        if (decisionReason != null && !decisionReason.isEmpty()) {
            root.add(Html.strong()
                    .text(getTranslation(I18n.Storefront.DECISION_REASON)))
                    .add(Html.span().text(": " + decisionReason))
                    .add(Html.br());
        }

        // Append purchase line items
        if (!purchase.getLines().isEmpty()) {
            root.add(Html.br());
            root.add(buildPurchaseLinesHtml(purchase));
        }

        return root;
    }

    /**
     * Builds an HTML fragment listing all purchase lines for the given
     * purchase. Each line includes product name, unit price, quantity and line
     * total.
     */
    private Html.Div buildPurchaseLinesHtml(Purchase purchase) {
        var container = Html.div();
        NumberFormat euroFormat = new DecimalFormat("#,##0.00 €");

        for (PurchaseLine line : purchase.getLines()) {
            var productName = line.getProduct() != null
                    ? line.getProduct().getProductName()
                    : "";
            var unitPrice = euroFormat.format(line.getUnitPrice());
            var quantity = line.getQuantity();
            var lineTotal = euroFormat.format(line.getLineTotal());

            var text = String.format("%s: %s x %d = %s", productName, unitPrice,
                    quantity, lineTotal);

            container.add(Html.span().text(text)).add(Html.br());
        }

        return container;
    }

    /**
     * Refreshes the grid data.
     */
    public void refresh() {
        grid.getDataProvider().refreshAll();
    }

    /**
     * Adds an action column to the end of the grid. The provided function
     * produces a component (e.g. buttons) for each row.
     *
     * @param actionProvider
     *            function that creates an action component per purchase row
     */
    public void setActionColumn(
            SerializableFunction<Purchase, Component> actionProvider) {
        Objects.requireNonNull(actionProvider,
                "Action provider must not be null");
        grid.addComponentColumn(actionProvider::apply).setId("actions")
                .setWidth(200);
    }

    /**
     * Button component for toggling the visibility of purchase details.
     * Displays an appropriate icon and ARIA label based on the current state of
     * the details visibility.
     */
    class ToggleButton extends Button implements HasAttributes<ToggleButton> {
        public ToggleButton(Purchase purchase) {
            setIcon(grid.isDetailsVisible(purchase) ? VaadinIcons.ANGLE_DOWN
                    : VaadinIcons.ANGLE_RIGHT);
            setAriaLabel(grid.isDetailsVisible(purchase)
                    ? getTranslation(I18n.Storefront.CLOSE)
                    : getTranslation(I18n.Storefront.OPEN));
            setAttribute(AriaAttributes.EXPANDED,
                    String.valueOf(grid.isDetailsVisible(purchase)));
            addStyleNames(ValoTheme.BUTTON_ICON_ONLY,
                    ValoTheme.BUTTON_BORDERLESS);
            addClickListener(e -> {
                grid.setDetailsVisible(purchase,
                        !grid.isDetailsVisible(purchase));
                setIcon(grid.isDetailsVisible(purchase) ? VaadinIcons.ANGLE_DOWN
                        : VaadinIcons.ANGLE_RIGHT);
                setAriaLabel(grid.isDetailsVisible(purchase)
                        ? getTranslation(I18n.Storefront.CLOSE)
                        : getTranslation(I18n.Storefront.OPEN));
                setAttribute(AriaAttributes.EXPANDED,
                        String.valueOf(grid.isDetailsVisible(purchase)));
            });
        }
    }
}
