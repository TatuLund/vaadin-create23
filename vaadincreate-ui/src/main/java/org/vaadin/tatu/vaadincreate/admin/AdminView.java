package org.vaadin.tatu.vaadincreate.admin;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.AbstractErrorMessage.ContentMode;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

// This is an example of a possible sub-navigation pattern in Vaadin 8
// using url parameters.
@NullMarked
@SuppressWarnings("serial")
@RolesPermitted({ Role.ADMIN })
public class AdminView extends VerticalLayout implements View, HasI18N {

    private static final Logger logger = LoggerFactory
            .getLogger(AdminView.class);

    public static final String VIEW_NAME = "admin";

    public AdminView() {
        addStyleName(VaadinCreateTheme.ADMINVIEW);
    }

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
        tabSheet.addSelectedTabChangeListener(tabChange -> {
            var selectedTab = tabSheet.getSelectedTab();
            String tabName = null;
            if (selectedTab instanceof TabView tabView) {
                tabName = tabView.getTabName();
                setFragmentParameter(tabName);
                tabView.enter();
                logger.info("Tab '{}' selected.", tabName);
            } else {
                logger.warn("Selected tab is not an instance of TabView: {}",
                        selectedTab.getClass().getName());
            }
            if (tabSheet.getTab(1).getComponent().getStyleName()
                    .contains(VaadinCreateTheme.ADMINVIEW_USERFORM_CHANGES)) {
                var errorMessage = new UserError(
                        getTranslation(I18n.User.CHANGES), ContentMode.TEXT,
                        ErrorLevel.WARNING);
                tabSheet.getTab(1).setComponentError(errorMessage);
            } else {
                tabSheet.getTab(1).setComponentError(null);
            }
        });
        if (params.equals(UserManagementView.VIEW_NAME)) {
            tabSheet.setSelectedTab(users);
            users.enter();
            logger.info("Tab 'users' selected.");
        }
        if (params.equals(CategoryManagementView.VIEW_NAME)
                || params.equals("")) {
            tabSheet.setSelectedTab(categories);
            categories.enter();
            logger.info("Tab 'categories' selected.");
        }
        removeAllComponents();
        addComponent(tabSheet);
        setSizeFull();
    }

    /**
     * Update the fragment without causing navigator to change view
     *
     * @param tabName
     *            The name of the tab to show
     */
    public void setFragmentParameter(@Nullable String tabName) {
        String fragmentParameter;
        if (tabName == null || tabName.isEmpty()) {
            fragmentParameter = "";
        } else {
            fragmentParameter = tabName;
        }

        var page = VaadinCreateUI.get().getPage();
        page.setUriFragment(
                String.format("!%s/%s", AdminView.VIEW_NAME, fragmentParameter),
                false);
    }

}
