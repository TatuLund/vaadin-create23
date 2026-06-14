package org.vaadin.tatu.vaadincreate.purchases;

import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Button component for toggling the visibility of purchase details. Displays an
 * appropriate icon and ARIA label based on the current state of the details
 * visibility.
 */
class ToggleButton extends Button
        implements HasAttributes<ToggleButton>, HasI18N {
    public ToggleButton(Grid<Purchase> grid, Purchase purchase) {
        setIcon(grid.isDetailsVisible(purchase) ? VaadinIcons.ANGLE_DOWN
                : VaadinIcons.ANGLE_RIGHT);
        setAriaLabel(grid.isDetailsVisible(purchase)
                ? getTranslation(I18n.Storefront.CLOSE)
                : getTranslation(I18n.Storefront.OPEN));
        setAttribute(AriaAttributes.EXPANDED,
                String.valueOf(grid.isDetailsVisible(purchase)));
        addStyleNames(ValoTheme.BUTTON_ICON_ONLY,
                ValoTheme.BUTTON_BORDERLESS);
        addClickListener(clickEvent -> {
            grid.setDetailsVisible(purchase,
                    !grid.isDetailsVisible(purchase));
            setIcon(grid.isDetailsVisible(purchase) ? VaadinIcons.ANGLE_DOWN
                    : VaadinIcons.ANGLE_RIGHT);
            setAriaLabel(grid.isDetailsVisible(purchase)
                    ? getTranslation(I18n.Storefront.CLOSE)
                    : getTranslation(I18n.Storefront.OPEN));
            setAttribute(AriaAttributes.EXPANDED,
                    String.valueOf(grid.isDetailsVisible(purchase)));
        });
    }
}
