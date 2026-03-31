package org.vaadin.tatu.vaadincreate.purchases;

import java.util.Objects;

import javax.persistence.OptimisticLockException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.observability.Telemetry;
import org.vaadin.tatu.vaadincreate.purchases.PurchasesApprovalsPresenter.ApproveResult;
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
public class PurchasesApprovalsView extends VerticalLayout
        implements TabView {

    public static final String VIEW_NAME = I18n.APPROVALS;

    private final PurchaseHistoryGrid approvalsGrid;
    private final PurchasesApprovalsPresenter presenter;

    public PurchasesApprovalsView() {
        setSizeFull();
        setMargin(false);
        presenter = new PurchasesApprovalsPresenter();
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
        button.addStyleNames(ValoTheme.BUTTON_PRIMARY, ValoTheme.BUTTON_SMALL);
        button.setDisableOnClick(true);
        button.addClickListener(
                clickEvent -> openDecisionDialog(purchase, true, button));
        return button;
    }

    private Button buildRejectButton(Purchase purchase) {
        var button = new Button(getTranslation(I18n.Storefront.REJECT));
        button.setId("reject-button-" + purchase.getId());
        button.addStyleNames(ValoTheme.BUTTON_DANGER, ValoTheme.BUTTON_SMALL);
        button.setDisableOnClick(true);
        button.addClickListener(
                clickEvent -> openDecisionDialog(purchase, false, button));
        return button;
    }

    private void openDecisionDialog(Purchase purchase, boolean isApprove,
            Button sourceButton) {
        var purchaseId = purchase.getId();
        if (purchaseId == null) {
            return;
        }
        var dialog = new DecisionDialog(isApprove,
                comment -> handleDecision(purchaseId, isApprove, comment));
        // Re-enable the source button only if the window was closed without
        // confirming (i.e. user cancelled), so they can try again.
        dialog.addCloseListener(closeEvent -> {
            if (!dialog.isConfirmed()) {
                sourceButton.setEnabled(true);
            }
            approvalsGrid.focus();
        });
        dialog.open();
    }

    private void handleDecision(Integer purchaseId, boolean isApprove,
            @Nullable String comment) {
        var currentUser = Utils.getCurrentUserOrThrow();
        if (isApprove) {
            handleApprove(purchaseId, currentUser, comment);
        } else {
            handleReject(purchaseId, currentUser,
                    Objects.requireNonNull(comment,
                            "Rejection reason must not be null"));
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
                                purchaseId, reason != null ? reason : ""),
                        Type.WARNING_MESSAGE);
            } else {
                Notification
                        .show(getTranslation(I18n.Storefront.APPROVE_SUCCESS,
                                purchaseId), Type.HUMANIZED_MESSAGE);
            }
            Telemetry.saveItem(result.purchase());
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
            String reason) {
        var purchase = presenter.reject(purchaseId, currentUser, reason);
        Telemetry.saveItem(purchase);
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
