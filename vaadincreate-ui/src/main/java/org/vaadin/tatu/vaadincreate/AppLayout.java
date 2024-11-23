package org.vaadin.tatu.vaadincreate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.AllPermitted;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Resource;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This is a responsive application shell with Navigator build with ValoMenu
 */
@SuppressWarnings({ "serial", "java:S2160" })
public class AppLayout extends Composite implements HasI18N {

    private final HorizontalLayout layout = new HorizontalLayout();
    private final VerticalLayout content = new VerticalLayout();
    private final CssLayout menuLayout = new CssLayout();
    private final CssLayout menuItems = new CssLayout();
    private final CssLayout title;
    private final UI ui;
    private AccessControl accessControl;

    /**
     * Constructor.
     *
     * @param ui
     *            The UI
     */
    public AppLayout(UI ui, AccessControl accessControl) {
        layout.setSpacing(false);
        layout.setMargin(false);
        this.accessControl = accessControl;
        this.ui = ui;
        ui.addStyleName(ValoTheme.UI_WITH_MENU);
        layout.addStyleName("applayout");
        Navigator nav = new Navigator(ui, content);
        nav.setErrorView(ErrorView.class);
        ui.setNavigator(nav);

        layout.setSizeFull();
        // Make the application responsive, see vaadincreate.scss
        Responsive.makeResponsive(ui);

        menuLayout.setPrimaryStyleName(ValoTheme.MENU_ROOT);
        menuLayout.addStyleName(ValoTheme.MENU_PART);
        menuLayout.setWidth(null);
        menuLayout.setHeight("100%");

        title = new CssLayout();
        var logo = new Label(VaadinIcons.BOOK.getHtml(), ContentMode.HTML);
        logo.setSizeUndefined();
        logo.setPrimaryStyleName(ValoTheme.MENU_LOGO);
        title.addComponents(logo);
        title.addStyleNames(ValoTheme.MENU_TITLE);

        menuLayout.addComponent(title);

        // Add a button to toggle the visibility of the menu when on mobile
        var toggleButton = new Button(getTranslation(I18n.App.MENU), event -> {
            if (menuLayout.getStyleName().contains(ValoTheme.MENU_VISIBLE)) {
                menuLayout.removeStyleName(ValoTheme.MENU_VISIBLE);
            } else {
                menuLayout.addStyleName(ValoTheme.MENU_VISIBLE);
            }
        });
        toggleButton.setDescription(getTranslation(I18n.App.MENU));

        toggleButton.setIcon(VaadinIcons.LIST);
        toggleButton.addStyleName(ValoTheme.MENU_TOGGLE);
        toggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        toggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
        menuLayout.addComponent(toggleButton);

        AttributeExtension.of(menuItems).setAttribute("role", "menu");
        menuLayout.addComponent(menuItems);
        menuItems.addStyleName(ValoTheme.MENU_ITEMS);

        var logout = new MenuBar();
        logout.setId("logout");
        var item = logout.addItem(getTranslation(I18n.App.LOGOUT),
                e -> handleConfirmLogoutWhenChanges(ui, nav));
        item.setIcon(VaadinIcons.KEY);
        item.setDescription(
                getTranslation(I18n.App.LOGOUT_TOOLTIP, getUserName()));
        logout.addStyleName(ValoTheme.MENU_USER);
        menuLayout.addComponent(logout);

        content.setPrimaryStyleName(ValoTheme.NAV_CONTENT);
        content.setSizeFull();
        content.setMargin(false);
        content.setSpacing(false);

        layout.addComponent(menuLayout);
        layout.addComponent(content);
        layout.setExpandRatio(content, 1);

        // Use view change listener to detect when navigation happened either by
        // user or directly using location. Update Menu's selected item based on
        // path. Also check if the user has access granted by @AccessAllowed
        // annotation, if not reject navigation to path.
        nav.addViewChangeListener(new ViewChangeListener() {
            @Override
            public void afterViewChange(ViewChangeEvent event) {
                clearSelected();
                setSelected(event.getViewName());
                logger.info("User '{}' navigated to view '{}'", getUserName(),
                        event.getViewName());
                menuLayout.removeStyleName(ValoTheme.MENU_VISIBLE);
            }

            @Override
            public boolean beforeViewChange(ViewChangeEvent event) {
                var view = event.getNewView();
                return hasAccessToView(view.getClass());
            }

        });
        setCompositionRoot(layout);
    }

    private void handleConfirmLogoutWhenChanges(UI ui, Navigator nav) {
        // Use runAfterLeaveConfirmation wrapper to run the logout in based
        // on beforeLeave of the current view. E.g. if BooksView has changes
        // ConfirmDialog is shown.
        nav.runAfterLeaveConfirmation(() -> {
            logger.info("User '{}' logout", getUserName());
            ui.getSession().close();
            ui.getPage().reload();
        });
    }

    // Check if the view has @AccessAllowed annotation. If the annotation exists
    // grant the access based on it.
    private boolean hasAccessToView(Class<? extends View> view) {
        var allPermitted = view.getAnnotation(AllPermitted.class);
        if (allPermitted != null) {
            return true;
        }
        var rolePermitted = view.getAnnotation(RolesPermitted.class);
        if (rolePermitted != null) {
            boolean canAccess = false;
            for (Role role : rolePermitted.value()) {
                if (accessControl.isUserInRole(role)) {
                    canAccess = true;
                    break;
                }
            }
            return canAccess;
        }
        logger.warn("User '{}' has no permission to view '{}'", getUserName(),
                view.getName());
        return false;
    }

    /**
     * Add a new view to application shell if the current user has access to it
     * based on @AccessAllowed annotation in the view.
     *
     * @param view
     *            The view class
     * @param viewName
     *            The name of the view shown in the menu
     * @param Icon
     *            Icon to be used in menu
     * @param path
     *            The name / uri path
     */
    public void addView(Class<? extends View> view, String viewName,
            Resource icon, String path) {
        if (!hasAccessToView(view)) {
            return;
        }
        var menuItem = new Button(viewName);
        menuItem.setId(path);
        menuItem.setData(path);
        menuItem.addClickListener(e -> ui.getNavigator().navigateTo(path));
        menuItem.setPrimaryStyleName(ValoTheme.MENU_ITEM);
        if (path.equals("")) {
            menuItem.addStyleName(ValoTheme.MENU_SELECTED);
        }
        ui.getNavigator().addView(path, view);
        menuItem.setIcon(icon);
        menuItems.addComponent(menuItem);
    }

    // Clear the menu
    private void clearSelected() {
        var iter = menuItems.iterator();
        while (iter.hasNext()) {
            iter.next().removeStyleName(ValoTheme.MENU_SELECTED);
        }
    }

    // Set the selected menu item by path
    private void setSelected(String path) {
        var iter = menuItems.iterator();
        while (iter.hasNext()) {
            var menuItem = iter.next();
            menuItem.removeStyleName(ValoTheme.MENU_SELECTED);
            if (((Button) menuItem).getData().toString().equals(path)) {
                menuItem.addStyleName(ValoTheme.MENU_SELECTED);
            }
        }
    }

    private static String getUserName() {
        return CurrentUser.get().isPresent() ? CurrentUser.get().get().getName()
                : "";
    }

    private static Logger logger = LoggerFactory.getLogger(AppLayout.class);
}
