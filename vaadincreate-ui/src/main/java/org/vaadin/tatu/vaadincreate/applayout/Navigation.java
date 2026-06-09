package org.vaadin.tatu.vaadincreate.applayout;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;

import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A navigation component for the application shell.
 */
@NullMarked
@SuppressWarnings("java:S2160")
class Navigation extends Composite implements HasAttributes<Navigation> {

    private final CssLayout items = new CssLayout();

    public Navigation() {
        setCompositionRoot(items);
        items.addStyleName(ValoTheme.MENU_ITEMS);
        setRole(AriaRoles.NAVIGATION);
        setAttribute(AriaAttributes.KEYSHORTCUTS, "Alt+Shift+N");
    }

    /**
     * Add a menu button to the navigation.
     * 
     * @param button
     *            A MenuButton to be added
     */
    void addMenuButton(MenuButton button) {
        items.addComponent(button);
    }

    /**
     * Clear the selected state of all menu items.
     */
    void clearSelected() {
        var iter = items.iterator();
        while (iter.hasNext()) {
            iter.next().removeStyleName(ValoTheme.MENU_SELECTED);
        }
    }

    /**
     * Set the selected state of the menu item based on the path.
     * 
     * @param path
     *            The path to set as selected
     */
    void setSelected(String path) {
        var iter = items.iterator();
        while (iter.hasNext()) {
            var menuItem = (MenuButton) iter.next();
            menuItem.setSelected(menuItem.getPath().equals(path));
        }
    }
}
