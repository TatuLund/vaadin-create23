package org.vaadin.tatu.vaadincreate.purchases;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Approvals tab placeholder for PurchasesView.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchasesApprovalsTab extends VerticalLayout implements TabView {

    public static final String VIEW_NAME = I18n.APPROVALS;

    public PurchasesApprovalsTab() {
        setSizeFull();
        var placeholder = new Label(getTranslation(I18n.APPROVALS));
        placeholder.setId("purchases-approvals-placeholder");
        addComponent(placeholder);
        setComponentAlignment(placeholder, Alignment.MIDDLE_CENTER);
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
