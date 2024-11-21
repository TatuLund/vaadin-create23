package org.vaadin.tatu.vaadincreate;

import com.vaadin.navigator.View;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ResetButtonForTextFieldView extends VerticalLayout
        implements View {
    public static final String NAME = "reset-button-for-text-field";

    public ResetButtonForTextFieldView() {
        var field = new TextField("Field");
        field.setValue("Value");
        ResetButtonForTextField.of(field);
        field.addValueChangeListener(e -> {
            Label label = new Label();
            label.setValue("Value:"+e.getValue());
            label.setId("value");
            addComponent(label);
        });
        addComponent(field);
    }
}
