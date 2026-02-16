package org.vaadin.tatu.vaadincreate.purchases;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.stats.StatsView;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Stats tab placeholder for PurchasesView.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchasesStatsTab extends VerticalLayout implements TabView {

    public static final String VIEW_NAME = StatsView.VIEW_NAME;

    public PurchasesStatsTab() {
        setSizeFull();
        var placeholder = new Label(getTranslation(StatsView.VIEW_NAME));
        placeholder.setId("purchases-stats-placeholder");
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
