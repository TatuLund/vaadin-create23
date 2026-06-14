package org.vaadin.tatu.vaadincreate.purchases;

import java.text.NumberFormat;
import java.time.Instant;

import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseLine;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.Html;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Label;

class PurchaseDetails extends Composite implements HasI18N {
    public PurchaseDetails(Purchase purchase, PurchaseHistoryMode mode) {
        Html.Div htmlDiv;
        if (mode == PurchaseHistoryMode.MY_PURCHASES) {
            htmlDiv = detailsHtmlForMyPurchase(purchase);
        } else {
            htmlDiv = buildPurchaseLinesHtml(purchase);
        }
        htmlDiv.attr(AriaAttributes.LIVE, "assertive");
        htmlDiv.attr(AriaAttributes.ROLE, AriaRoles.ALERT);
        var label = new Label(htmlDiv.build(), ContentMode.HTML);
        setCompositionRoot(label);
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

}
