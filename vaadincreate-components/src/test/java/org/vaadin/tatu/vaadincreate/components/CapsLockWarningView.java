package org.vaadin.tatu.vaadincreate.components;

import com.vaadin.navigator.View;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class CapsLockWarningView extends VerticalLayout implements View {
    public static final String NAME = "capslock-warning";

    public CapsLockWarningView() {
        var field = new PasswordField("Password");
        var capsLock = CapsLockWarning.warnFor(field);
        capsLock.setMessage("Caps Lock");
        addComponent(field);
    }
}
