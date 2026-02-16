package org.vaadin.tatu.vaadincreate.purchases;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.PurchaseHistoryMode;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.ui.VerticalLayout;

/**
 * History tab for PurchasesView.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchasesHistoryTab extends VerticalLayout implements TabView {

    public static final String VIEW_NAME = I18n.Storefront.PURCHASE_HISTORY;

    private final PurchaseHistoryGrid historyGrid;

    public PurchasesHistoryTab() {
        setSizeFull();
        setMargin(false);
        historyGrid = new PurchaseHistoryGrid(new PurchaseHistoryPresenter(),
                PurchaseHistoryMode.ALL, Utils.getCurrentUserOrThrow());
        addComponent(historyGrid);
        setExpandRatio(historyGrid, 1);
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
