package org.vaadin.tatu.vaadincreate.purchases;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.common.EuroRenderer;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.components.Html;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.provider.CallbackDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializableFunction;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.grid.ScrollDestination;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Grid component for displaying purchase history. Supports pagination via
 * callback data provider.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchaseHistoryGrid extends Composite implements HasI18N {
    private final Grid<Purchase> grid = new Grid<>();
    private final PurchaseHistoryPresenter presenter;
    private final PurchaseHistoryMode mode;
    private final User currentUser;
    @Nullable
    private Instant oldHighlightCutoff;
    @Nullable
    private Instant selectedRangeFrom;
    @Nullable
    private Instant selectedRangeToExclusive;

    @Nullable
    private UI ui;

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

        presenter.register(this, currentUser);
        addDetachListener(e -> presenter.unregister());
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
        grid.addItemClickListener(clickEvent -> {
            if (clickEvent.getItem() != null) {
                grid.setDetailsVisible(clickEvent.getItem(),
                        !grid.isDetailsVisible(clickEvent.getItem()));
                grid.getDataProvider().refreshItem(clickEvent.getItem());
            }
        });
    }

    private void configureColumns() {
        grid.addComponentColumn(ToggleButton::new).setWidth(50);

        var idColumn = grid.addColumn(Purchase::getId)
                .setCaption(getTranslation(I18n.Storefront.PURCHASE_ID))
                .setSortable(false).setId("purchase-id");

        var requesterColumn = grid.addColumn(p -> p.getRequester().getName())
                .setCaption(getTranslation(I18n.REQUESTER)).setSortable(false)
                .setId("requester");

        var approverColumn = grid
                .addColumn(p -> p.getApprover().getName())
                .setCaption(getTranslation(I18n.Storefront.APPROVER))
                .setSortable(false).setId("approver");

        grid.addColumn(
                purchase -> Utils.formatDateTime(purchase.getCreatedAt()))
                .setCaption(getTranslation(I18n.CREATED_AT)).setId("created-at")
                .setSortable(false);

        grid.addColumn(Purchase::getStatus)
                .setCaption(getTranslation(I18n.STATUS)).setSortable(false)
                .setId("status");

        var decidedAtColumn = grid.addColumn(purchase -> {
            Instant decidedAt = purchase.getDecidedAt();
            return decidedAt != null ? Utils.formatDateTime(decidedAt) : "";
        }).setCaption(getTranslation(I18n.Storefront.DECIDED_AT))
                .setId("decided-at").setSortable(false);

        grid.addColumn(Purchase::getTotalAmount, new EuroRenderer())
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
        } else if (mode == PurchaseHistoryMode.PENDING_APPROVALS) {
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
                }, query -> (int) presenter.countPurchases(mode, currentUser));

        grid.setDataProvider(dataProvider);
    }

    /**
     * Scrolls the grid to the given index from the bottom.
     *
     * @param index
     *            integer index to scroll to from the bottom
     */
    @SuppressWarnings("java:S4274")
    public void scrollToFromBottom(int index) {
        var targetIndex = grid.getDataCommunicator().getDataProviderSize()
                - index;
        assert targetIndex >= 0 : "Target index must be non-negative";
        grid.scrollTo(targetIndex, ScrollDestination.START);
    }

    /**
     * Scrolls the grid to an absolute row index.
     *
     * @param index
     *            zero-based index
     */
    public void scrollToIndex(int index) {
        if (index < 0) {
            return;
        }
        grid.scrollTo(index, ScrollDestination.START);
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

        root.add(
                Html.strong().text(getTranslation(I18n.Storefront.PURCHASE_ID)))
                .add(Html.span().text(": " + purchase.getId())).add(Html.br());

        var approver = purchase.getApprover();
        assert approver != null : "Approver cannot be null";
        root.add(Html.strong().text(getTranslation(I18n.Storefront.APPROVER)))
                .add(Html.span().text(": " + approver.getName()))
                .add(Html.br());

        Instant decidedAt = purchase.getDecidedAt();
        var decidedAtValue = decidedAt != null ? Utils.formatDateTime(decidedAt)
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
        NumberFormat euroFormat = EuroConverter.createEuroFormat();

        for (PurchaseLine line : purchase.getLines()) {
            var productName = line.getProduct().getProductName();
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
     * Applies or removes the old-purchase row style. Rows whose
     * {@code createdAt} is before {@code cutoff} receive the CSS class
     * {@link VaadinCreateTheme#PURCHASE_OLD}. Pass {@code null} to clear the
     * style generator (no rows highlighted).
     *
     * @param cutoff
     *            the exclusive upper-bound instant; {@code null} clears
     *            highlighting
     */
    public void setOldPurchaseHighlight(@Nullable Instant cutoff) {
        oldHighlightCutoff = cutoff;
        applyRowStyleGenerator();
    }

    /**
     * Applies a highlight class for purchases in the selected range
     * [fromInclusive, toExclusive). Passing null clears range highlighting.
     *
     * @param fromInclusive
     *            inclusive start instant
     * @param toExclusive
     *            exclusive end instant
     */
    public void setSelectedRangeHighlight(@Nullable Instant fromInclusive,
            @Nullable Instant toExclusive) {
        selectedRangeFrom = fromInclusive;
        selectedRangeToExclusive = toExclusive;
        applyRowStyleGenerator();
    }

    private void applyRowStyleGenerator() {
        grid.setStyleGenerator(p -> {
            var createdAt = p.getCreatedAt();
            if (createdAt == null) {
                return null;
            }
            var style = new StringBuilder();
            if (oldHighlightCutoff != null && createdAt.isBefore(oldHighlightCutoff)) {
                style.append(VaadinCreateTheme.PURCHASE_OLD);
            }
            if (selectedRangeFrom != null && selectedRangeToExclusive != null
                    && !createdAt.isBefore(selectedRangeFrom)
                    && createdAt.isBefore(selectedRangeToExclusive)) {
                if (!style.isEmpty()) {
                    style.append(" ");
                }
                style.append(VaadinCreateTheme.PURCHASE_RANGE);
            }
            return style.isEmpty() ? null : style.toString();
        });
    }

    /**
     * Refreshes the grid data.
     */
    public void refresh() {
        grid.getDataProvider().refreshAll();
    }

    @Override
    public void attach() {
        super.attach();
        ui = getUI();
    }

    /**
     * Refreshes all grid data asynchronously using {@link Utils#access}. Safe
     * to call from non-UI threads (e.g. EventBus callbacks).
     */
    public void refreshAsync() {
        Utils.access(ui, this::refresh);
    }

    public void refreshItemAsync(Purchase purchase) {
        Utils.access(ui, () -> grid.getDataProvider().refreshItem(purchase));
    }

    /**
     * Shows a tray notification with the purchase status change message. Uses
     * {@link Utils#access} so it is safe to call from non-UI threads.
     *
     * @param purchase
     *            the purchase whose status changed
     */
    public void showStatusNotificationAsync(Purchase purchase) {
        Utils.access(ui, () -> {
            String message = buildStatusMessage(purchase);
            Notification.show(message, Type.TRAY_NOTIFICATION);
        });
    }

    private String buildStatusMessage(Purchase purchase) {
        var id = purchase.getId() != null ? purchase.getId() : "";
        var reason = purchase.getDecisionReason() != null
                ? purchase.getDecisionReason()
                : "";
        return switch (purchase.getStatus()) {
        case COMPLETED -> getTranslation(
                I18n.Storefront.PURCHASE_STATUS_APPROVED, id);
        case REJECTED -> getTranslation(
                I18n.Storefront.PURCHASE_STATUS_REJECTED, id, reason);
        case CANCELLED -> getTranslation(
                I18n.Storefront.PURCHASE_STATUS_CANCELLED, id, reason);
        default -> "";
        };
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
            addClickListener(clickEvent -> {
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
