package org.vaadin.tatu.vaadincreate.purchases;

import javax.persistence.OptimisticLockException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.purchases.ApprovalsPresenter.ApproveResult;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Approvals tab for PurchasesView. Displays pending purchases assigned to the
 * current user with approve and reject action buttons.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchasesApprovalsTab extends VerticalLayout
        implements TabView, HasI18N {

    public static final String VIEW_NAME = I18n.APPROVALS;

    private final PurchaseHistoryGrid approvalsGrid;
    private final ApprovalsPresenter presenter;

    public PurchasesApprovalsTab() {
        setSizeFull();
        setMargin(false);
        presenter = new ApprovalsPresenter();
        approvalsGrid = new PurchaseHistoryGrid(new PurchaseHistoryPresenter(),
                PurchaseHistoryMode.PENDING_APPROVALS,
                Utils.getCurrentUserOrThrow());
        approvalsGrid.setActionColumn(this::createActionButtons);
        addComponent(approvalsGrid);
        setExpandRatio(approvalsGrid, 1);
    }

    private Component createActionButtons(Purchase purchase) {
        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            return new HorizontalLayout();
        }
        var approveButton = buildApproveButton(purchase);
        var rejectButton = buildRejectButton(purchase);
        var layout = new HorizontalLayout(approveButton, rejectButton);
        layout.setSpacing(true);
        return layout;
    }

    private Button buildApproveButton(Purchase purchase) {
        var button = new Button(getTranslation(I18n.Storefront.APPROVE));
        button.setId("approve-button-" + purchase.getId());
        button.addStyleName(ValoTheme.BUTTON_PRIMARY);
        button.setDisableOnClick(true);
        button.addClickListener(
                e -> openDecisionWindow(purchase, true, button));
        return button;
    }

    private Button buildRejectButton(Purchase purchase) {
        var button = new Button(getTranslation(I18n.Storefront.REJECT));
        button.setId("reject-button-" + purchase.getId());
        button.addStyleName(ValoTheme.BUTTON_DANGER);
        button.setDisableOnClick(true);
        button.addClickListener(
                e -> openDecisionWindow(purchase, false, button));
        return button;
    }

    private void openDecisionWindow(Purchase purchase, boolean isApprove,
            Button sourceButton) {
        var purchaseId = purchase.getId();
        if (purchaseId == null) {
            return;
        }
        var window = new DecisionWindow(isApprove,
                comment -> handleDecision(purchaseId, isApprove, comment));
        // Re-enable the source button only if the window was closed without
        // confirming (i.e. user cancelled), so they can try again.
        // The cast to Serializable is required because Window.CloseListener
        // does not extend Serializable and lambdas must be serializable in
        // Vaadin 8 sessions.
        window.addCloseListener(
                (com.vaadin.ui.Window.CloseListener & java.io.Serializable) e -> {
                    if (!window.isConfirmed()) {
                        sourceButton.setEnabled(true);
                    }
                });
        getUI().addWindow(window);
        window.center();
    }

    private void handleDecision(Integer purchaseId, boolean isApprove,
            @Nullable String comment) {
        var currentUser = Utils.getCurrentUserOrThrow();
        if (isApprove) {
            handleApprove(purchaseId, currentUser, comment);
        } else {
            handleReject(purchaseId, currentUser, comment);
        }
    }

    private void handleApprove(Integer purchaseId, User currentUser,
            @Nullable String comment) {
        try {
            ApproveResult result = presenter.approve(purchaseId, currentUser,
                    comment);
            if (result.isCancelled()) {
                var reason = result.purchase().getDecisionReason();
                Notification.show(
                        getTranslation(I18n.Storefront.INSUFFICIENT_STOCK,
                                purchaseId,
                                reason != null ? reason : ""),
                        Type.WARNING_MESSAGE);
            } else {
                Notification.show(
                        getTranslation(I18n.Storefront.APPROVE_SUCCESS,
                                purchaseId),
                        Type.HUMANIZED_MESSAGE);
            }
        } catch (Exception e) {
            if (Utils.throwableHasCause(e, OptimisticLockException.class)) {
                Notification.show(
                        getTranslation(I18n.Storefront.APPROVE_CONFLICT),
                        Type.WARNING_MESSAGE);
            } else {
                throw e;
            }
        }
        approvalsGrid.refresh();
    }

    private void handleReject(Integer purchaseId, User currentUser,
            @Nullable String reason) {
        if (reason == null || reason.isBlank()) {
            Notification.show(
                    getTranslation(I18n.Storefront.REASON_REQUIRED),
                    Type.WARNING_MESSAGE);
            return;
        }
        presenter.reject(purchaseId, currentUser, reason);
        Notification.show(
                getTranslation(I18n.Storefront.REJECT_SUCCESS, purchaseId),
                Type.HUMANIZED_MESSAGE);
        approvalsGrid.refresh();
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(event);
    }

    @Override
    public String getTabName() {
        return VIEW_NAME;
    }
}
