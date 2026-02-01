package org.vaadin.tatu.vaadincreate.admin;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

// This is an example of a possible sub-navigation pattern in Vaadin 8
// using url parameters.
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@RolesPermitted({ Role.ADMIN })
public class AdminView extends VerticalLayout implements View, HasI18N {

    public static final String VIEW_NAME = "admin";

    @Nullable
    private TabNavigator tabNavigator;

    @Nullable
    private CategoryManagementView categories;

    @Nullable
    private UserManagementView users;

    public AdminView() {
        addStyleName(VaadinCreateTheme.ADMINVIEW);
        setSizeFull();
    }

    @Override
    public void enter(ViewChangeEvent event) {
        var params = event.getParameters();
        if (tabNavigator == null) {
            setupTabNavigator();
        }
        if (params.equals("")) {
            tabNavigator.navigate(categories);
        } else {
            var attempted = VIEW_NAME + "/" + params;
            try {
                tabNavigator.navigate(params);
            } catch (IllegalArgumentException e) {
                var ui = UI.getCurrent();
                ui.getNavigator().navigateTo("error/" + attempted);
                ui.getPage().setUriFragment("!" + attempted, false);
            }
        }
    }

    private void setupTabNavigator() {
        categories = new CategoryManagementView();
        users = new UserManagementView();
        tabNavigator = new TabNavigator(VIEW_NAME);
        tabNavigator.addTabView(categories,
                getTranslation(CategoryManagementView.VIEW_NAME),
                VaadinIcons.LIST);
        tabNavigator.addTabView(users,
                getTranslation(UserManagementView.VIEW_NAME),
                VaadinIcons.USERS);
        tabNavigator.addViewChangeListener(tabChange -> {
            if (users.getStyleName()
                    .contains(VaadinCreateTheme.ADMINVIEW_USERFORM_CHANGES)) {
                var errorText = getTranslation(I18n.User.CHANGES);
                tabNavigator.setComponentError(users, errorText);
            } else {
                tabNavigator.clearComponentError(users);
            }
        });
        addComponent(tabNavigator);
    }

}
