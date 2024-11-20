package org.vaadin.tatu.vaadincreate;

import com.vaadin.navigator.View;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class AttributeExtensionView extends VerticalLayout implements View {
    public static final String NAME = "attribute-extension";

    public AttributeExtensionView() {
        var field = new TextField("Number");
        var extension = AttributeExtension.of(field);
        extension.setAttribute("type", "number");
        extension.setAttribute("custom", "custom");
        extension.setAttribute("special", "sticky");

        var button = new Button("Remove", e -> {
            extension.removeAttribute("type");
            extension.removeAttribute("custom");
        });

        addComponents(field, button);
    }
}
