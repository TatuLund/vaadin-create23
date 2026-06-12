package org.vaadin.tatu.vaadincreate.applayout;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A button for the application menu.
 */
@NullMarked
@SuppressWarnings("java:S2160")
public class MenuButton extends Button
        implements HasAttributes<MenuButton>, HasI18N {

    private String path;
    private String caption;

    /**
     * Constructor.
     *
     * @param caption
     *            The caption of the button
     * @param path
     *            The path of the view
     * @param icon
     *            The icon to be used in the menu item
     */
    public MenuButton(String caption, String path, Resource icon) {
        super(caption);
        this.path = path;
        this.caption = caption;
        setId(path);
        setData(path);
        addClickListener(click -> getUI().getNavigator().navigateTo(path));
        setPrimaryStyleName(ValoTheme.MENU_ITEM);
        if (path.equals("")) {
            addStyleName(ValoTheme.MENU_SELECTED);
        }
        setIcon(icon);
        setRole(AriaRoles.LINK);
    }

    /**
     * Get the path of the menu item.
     *
     * @return the path of the menu item
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the selected state of the menu item. This will add or remove the
     * {@link ValoTheme#MENU_SELECTED} style name.
     * 
     * @param selected
     *            boolean value
     */
    public void setSelected(boolean selected) {
        if (selected) {
            addStyleName(ValoTheme.MENU_SELECTED);
            setAriaLabel(String.format("%s %s", caption,
                    getTranslation(I18n.CURRENT_PAGE)));
        } else {
            removeStyleName(ValoTheme.MENU_SELECTED);
            setAriaLabel(caption);
        }
    }
}
