package org.vaadin.tatu.vaadincreate;

import com.vaadin.navigator.View;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class AttributeExtensionView extends VerticalLayout implements View {
    public static final String NAME = "attribute-extension";

    public AttributeExtensionView() {
        var field = new AttributeTextField("Number");
        field.setAttribute("type", "number");
        field.setAttribute("custom", "custom");
        field.setAttribute("special", "sticky");

        var button = new Button("Remove", e -> {
            field.removeAttribute("type");
            field.removeAttribute("custom");
        });

        addComponents(field, button);
    }

    public static class AttributeTextField extends TextField
            implements AttributeExtension.HasAttributes<AttributeTextField> {

        public AttributeTextField(String caption) {
            super(caption);
        }
    }
}