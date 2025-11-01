package org.vaadin.tatu.vaadincreate.components;

import com.vaadin.navigator.View;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class CharacterCountExtensionView extends VerticalLayout
        implements View {
    public static final String NAME = "character-count-extension";

    public CharacterCountExtensionView() {
        var field = new TextField("Field");
        field.setWidth("300px");
        field.setMaxLength(20);
        field.setValue("This is a test value");;
        CharacterCountExtension.extend(field);
        var area = new TextArea("Area");
        area.setMaxLength(100);
        area.setWidth("300px");
        area.setValue("This is a test value");
        CharacterCountExtension.extend(area);
        addComponents(field, area);
    }
}
