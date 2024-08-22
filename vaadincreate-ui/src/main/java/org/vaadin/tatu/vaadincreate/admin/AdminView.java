package org.vaadin.tatu.vaadincreate.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

// This is an example of a possible sub-navigation pattern in Vaadin 8
// using url parameters.
@SuppressWarnings("serial")
@RolesPermitted({ Role.ADMIN })
public class AdminView extends VerticalLayout implements View, HasI18N {

    public static final String VIEW_NAME = "admin";

    @Override
    public void enter(ViewChangeEvent event) {
        var params = event.getParameters();
        var tabSheet = new TabSheet();
        var categories = new CategoryManagementView();
        var users = new UserManagementView();
        tabSheet.addTab(categories,
                getTranslation(CategoryManagementView.VIEW_NAME));
        tabSheet.getTab(categories).setIcon(VaadinIcons.LIST);
        tabSheet.addTab(users, getTranslation(UserManagementView.VIEW_NAME));
        tabSheet.getTab(users).setIcon(VaadinIcons.USERS);
        tabSheet.setSizeFull();
        tabSheet.addStyleNames(ValoTheme.TABSHEET_PADDED_TABBAR,
                ValoTheme.TABSHEET_CENTERED_TABS);
        tabSheet.addSelectedTabChangeListener(e -> {
            var tabName = ((TabView) tabSheet.getSelectedTab()).getTabName();
            setFragmentParameter(tabName);
            ((TabView) tabSheet.getSelectedTab()).enter();
            logger.info("Tab '{}' selected.", tabName);
        });
        if (params.equals(UserManagementView.VIEW_NAME)) {
            tabSheet.setSelectedTab(users);
        }
        if (params.equals(CategoryManagementView.VIEW_NAME)) {
            tabSheet.setSelectedTab(categories);
        }
        removeAllComponents();
        addComponent(tabSheet);
        categories.enter();
        logger.info("Tab 'categories' selected.");
        setSizeFull();
    }

    /**
     * Update the fragment without causing navigator to change view
     *
     * @param tabName
     *            The name of the tab to show
     */
    public void setFragmentParameter(String tabName) {
        String fragmentParameter;
        if (tabName == null || tabName.isEmpty()) {
            fragmentParameter = "";
        } else {
            fragmentParameter = tabName;
        }

        var page = VaadinCreateUI.get().getPage();
        page.setUriFragment("!" + AdminView.VIEW_NAME + "/" + fragmentParameter,
                false);
    }

    private static Logger logger = LoggerFactory.getLogger(AdminView.class);
}
