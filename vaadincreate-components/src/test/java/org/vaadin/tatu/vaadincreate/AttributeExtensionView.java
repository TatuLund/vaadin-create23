package org.vaadin.tatu.vaadincreate;

import com.vaadin.navigator.View;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class AttributeExtensionView extends VerticalLayout implements View {
    public static final String NAME = "attribute-extension";

    public AttributeExtensionView() {
        var field = new TextField("Number");
        var extension = new AttributeExtension();
        extension.extend(field);
        extension.setAttribute("type", "number");
        addComponent(field);
    }
}
