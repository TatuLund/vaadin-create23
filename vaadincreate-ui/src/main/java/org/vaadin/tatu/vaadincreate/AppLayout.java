package org.vaadin.tatu.vaadincreate;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.AttributeExtension.HasAttributes;
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
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This is a responsive application shell with Navigator build with ValoMenu
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class AppLayout extends Composite implements HasI18N {

    private final HorizontalLayout layout = new HorizontalLayout();
    private final VerticalLayout content = new VerticalLayout();
    private final CssLayout menuLayout = new CssLayout();
    private final CssLayout menuItems = new CssLayout();
    private final CssLayout title;
    private final UI ui;
    private AccessControl accessControl;
    private Label announcer;

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
        var resource = new ThemeResource("images/bookstore.png");
        var image = new Image("", resource);
        image.setPrimaryStyleName(ValoTheme.MENU_LOGO);
        image.setWidthUndefined();
        title.addComponents(image);
        title.addStyleNames(ValoTheme.MENU_TITLE);

        menuLayout.addComponent(title);

        // Add a button to toggle the visibility of the menu when on mobile
        var toggleButton = new Button(getTranslation(I18n.App.MENU), click -> {
            if (menuLayout.getStyleName().contains(ValoTheme.MENU_VISIBLE)) {
                menuLayout.removeStyleName(ValoTheme.MENU_VISIBLE);
                Notification.show(getTranslation(I18n.App.MENU_CLOSE),
                        Type.ASSISTIVE_NOTIFICATION);
            } else {
                menuLayout.addStyleName(ValoTheme.MENU_VISIBLE);
                Notification.show(getTranslation(I18n.App.MENU_OPEN),
                        Type.ASSISTIVE_NOTIFICATION);
            }
        });
        toggleButton.setDescription(getTranslation(I18n.App.MENU));

        toggleButton.setIcon(VaadinIcons.LIST);
        toggleButton.addStyleName(ValoTheme.MENU_TOGGLE);
        toggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        toggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
        menuLayout.addComponent(toggleButton);

        AttributeExtension.of(menuItems).setAttribute("role", "navigation");
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
            public void afterViewChange(ViewChangeEvent afterChange) {
                String viewName = afterChange.getViewName();
                if (viewName.isEmpty()) {
                    viewName = "about";
                }
                clearSelected();
                setSelected(viewName);
                logger.info("User '{}' navigated to view '{}'", getUserName(),
                        viewName);
                menuLayout.removeStyleName(ValoTheme.MENU_VISIBLE);
            }

            @Override
            public boolean beforeViewChange(ViewChangeEvent beforeChange) {
                var view = beforeChange.getNewView();
                return hasAccessToView(view.getClass());
            }

        });

        announcer = new Label();
        announcer.setContentMode(ContentMode.HTML);
        announcer.setPrimaryStyleName("announcer");
        layout.addComponent(announcer);

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
        var menuItem = new MenuButton(viewName, path, icon);
        ui.getNavigator().addView(path, view);
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
            var menuItem = (MenuButton) iter.next();
            menuItem.setSelected(menuItem.getPath().equals(path));
        }
    }

    private static String getUserName() {
        var user = CurrentUser.get();
        return user.isPresent() ? user.get().getName() : "";
    }

    public class MenuButton extends Button implements HasAttributes {

        private String path;
        private String caption;
        private AttributeExtension attributes;

        public MenuButton(String caption, String path, Resource icon) {
            super(caption);
            this.path = path;
            this.caption = caption;
            setId(path);
            setData(path);
            addClickListener(click -> ui.getNavigator().navigateTo(path));
            setPrimaryStyleName(ValoTheme.MENU_ITEM);
            if (path.equals("")) {
                addStyleName(ValoTheme.MENU_SELECTED);
            }
            setIcon(icon);
            attributes = AttributeExtension.of(this);
            setAttribute("role", "link");
        }

        public String getPath() {
            return path;
        }

        public void setSelected(boolean selected) {
            if (selected) {
                addStyleName(ValoTheme.MENU_SELECTED);
                setAttribute("aria-label",
                        caption + " " + getTranslation(I18n.CURRENT_PAGE));
            } else {
                removeStyleName(ValoTheme.MENU_SELECTED);
                setAttribute("aria-label", caption);
            }
        }

        @Override
        public AttributeExtension getAttributeExtension() {
            return attributes;
        }
    }

    private static Logger logger = LoggerFactory.getLogger(AppLayout.class);
}
