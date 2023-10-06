package org.vaadin.tatu.vaadincreate;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This is a responsive application shell with Navigator build wiht ValoMenu
 */
@SuppressWarnings("serial")
public class AppLayout extends HorizontalLayout {

    private final VerticalLayout content = new VerticalLayout();
    private final CssLayout menu = new CssLayout();
    private final CssLayout menuItems = new CssLayout();
    private final CssLayout title;
    private final UI ui;

    /**
     * Constructor.
     * 
     * @param ui
     *            The UI
     */
    public AppLayout(UI ui) {
        this.ui = ui;
        ui.addStyleName(ValoTheme.UI_WITH_MENU);
        addStyleName("applayout");
        Navigator nav = new Navigator(ui, content);
        nav.setErrorView(ErrorView.class);
        ui.setNavigator(nav);

        setSizeFull();
        Responsive.makeResponsive(ui);

        menu.setPrimaryStyleName(ValoTheme.MENU_ROOT);
        menu.addStyleName(ValoTheme.MENU_PART);
        menu.setWidth(null);
        menu.setHeight("100%");

        title = new CssLayout();
        var logo = new Label(VaadinIcons.BOOK.getHtml(), ContentMode.HTML);
        logo.setSizeUndefined();
        logo.setPrimaryStyleName(ValoTheme.MENU_LOGO);
        title.addComponents(logo);
        title.addStyleNames(ValoTheme.MENU_TITLE);

        menu.addComponent(title);

        var toggleButton = new Button("Menu", event -> {
            if (menu.getStyleName().contains(ValoTheme.MENU_VISIBLE)) {
                menu.removeStyleName(ValoTheme.MENU_VISIBLE);
            } else {
                menu.addStyleName(ValoTheme.MENU_VISIBLE);
            }
        });

        toggleButton.setIcon(VaadinIcons.LIST);
        toggleButton.addStyleName(ValoTheme.MENU_TOGGLE);
        toggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        toggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
        menu.addComponent(toggleButton);

        menu.addComponent(menuItems);
        menuItems.addStyleName(ValoTheme.MENU_ITEMS);

        var logout = new MenuBar();
        var item = logout.addItem("Logout", e -> {
            ui.getSession().getSession().invalidate();
            ui.getPage().reload();
        });
        item.setIcon(VaadinIcons.KEY);
        item.setDescription("Logout");
        logout.addStyleName("user-menu");
        menu.addComponent(logout);

        content.setPrimaryStyleName(ValoTheme.NAV_CONTENT);
        content.setSizeFull();
        content.setMargin(false);
        content.setSpacing(false);

        addComponent(menu);
        addComponent(content);
        setExpandRatio(content, 1);

        // Use view change listener to detect when navigation happened either by
        // user or directly using location. Update Menu's selected item based on
        // path.
        nav.addViewChangeListener(new ViewChangeListener() {
            @Override
            public void afterViewChange(ViewChangeEvent event) {
                clearSelected();
                setSelected(event.getViewName());
            }

            @Override
            public boolean beforeViewChange(ViewChangeEvent event) {
                return true;
            }
        });
    }

    /**
     * Add a new view to application shell
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
            Resource Icon, String path) {
        var menuItem = new Button(viewName);
        menuItem.setData(path);
        menuItem.addClickListener(e -> {
            ui.getNavigator().navigateTo(path);
        });
        menuItem.setPrimaryStyleName(ValoTheme.MENU_ITEM);
        if (path.equals("")) {
            menuItem.addStyleName(ValoTheme.MENU_SELECTED);
        }
        ui.getNavigator().addView(path, view);
        menuItem.setIcon(Icon);
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

}
