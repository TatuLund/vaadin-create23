package org.vaadin.tatu.vaadincreate.uiunittest.testers;

import org.vaadin.tatu.vaadincreate.uiunittest.Tester;

import com.vaadin.ui.Button;

public class ButtonTester extends Tester<Button> {

    public ButtonTester(Button component) {
        super(component);
    }

    /**
     * Assert that Button is enabled and produce simulated ClickEvent if it is.
     */
    public void click() {
        assert (getComponent().isEnabled()) : "Button is not enabled";
        getComponent().click();
    }
}
