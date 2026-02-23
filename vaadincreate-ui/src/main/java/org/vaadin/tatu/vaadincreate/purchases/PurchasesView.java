package org.vaadin.tatu.vaadincreate.purchases;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.VaadinCreateView;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.common.TabNavigator;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.stats.StatsView;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.VerticalLayout;

/**
 * View for user/admin purchase history and approvals.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@RolesPermitted({ Role.USER, Role.ADMIN })
public class PurchasesView extends VerticalLayout implements VaadinCreateView {

    public static final String VIEW_NAME = "purchases";

    @Nullable
    private TabNavigator tabNavigator;

    @Nullable
    private PurchasesHistoryView historyTab;

    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    @SuppressWarnings("java:S2637")
    public PurchasesView() {
        setSizeFull();
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(VIEW_NAME);
        var params = event.getParameters();
        if (tabNavigator == null) {
            setupTabNavigator();
        }
        assert tabNavigator != null : "Tab navigator must not be null";
        if (params.equals("")) {
            assert historyTab != null : "History tab must not be null";
            tabNavigator.navigate(historyTab);
        } else {
            tabNavigator.navigate(params);
        }
    }

    private void setupTabNavigator() {
        historyTab = new PurchasesHistoryView();
        var approvalsTab = new PurchasesApprovalsView();
        var statsTab = new PurchasesStatsView();
        tabNavigator = new TabNavigator(VIEW_NAME);
        if (accessControl.isUserInRole(Role.ADMIN)) {
            tabNavigator.addTabView(historyTab,
                    getTranslation(I18n.Storefront.PURCHASE_HISTORY),
                    VaadinIcons.TIME_BACKWARD);
        }
        tabNavigator.addTabView(approvalsTab,
                getTranslation(I18n.APPROVALS), VaadinIcons.CHECK);
        tabNavigator.addTabView(statsTab,
                getTranslation(StatsView.VIEW_NAME), VaadinIcons.CHART);
        addComponent(tabNavigator);
        setExpandRatio(tabNavigator, 1);
    }
}
