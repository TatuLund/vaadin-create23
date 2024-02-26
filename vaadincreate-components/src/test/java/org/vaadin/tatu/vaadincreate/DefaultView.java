package org.vaadin.tatu.vaadincreate;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class DefaultView extends VerticalLayout implements View {

    public DefaultView() {
        addComponent(new Label("Menu"));
    }

    @Override
    public void enter(ViewChangeEvent event) {
        var ui = ((ComponentsUI) getUI());
        ui.getViews().keySet().forEach(view -> {
            var button = new Button(view, e -> ui.navigate(view));
            button.addStyleNames(ValoTheme.BUTTON_LINK,
                    ValoTheme.BUTTON_BORDERLESS);
            addComponent(button);
        });
    }
}
