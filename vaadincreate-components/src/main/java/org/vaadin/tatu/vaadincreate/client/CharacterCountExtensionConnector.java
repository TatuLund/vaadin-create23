package org.vaadin.tatu.vaadincreate.client;

import org.vaadin.tatu.vaadincreate.CharacterCountExtension;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.VTextField;
import com.vaadin.client.ui.textfield.AbstractTextFieldConnector;
import com.vaadin.shared.ui.Connect;

@SuppressWarnings({ "serial", "java:S1948" })
@Connect(CharacterCountExtension.class)
public class CharacterCountExtensionConnector extends AbstractExtensionConnector
        implements KeyUpHandler, AttachEvent.Handler {

    public static final String CLASSNAME = "v-charactercount";

    private static final String BACKGROUND = "background";
    private static final String WIDTH = "width";
    private AbstractTextFieldConnector textFieldConnector;
    private VTextField textField;
    private Element countElement;
    private Element wrapper;

    @Override
    protected void extend(ServerConnector serverConnector) {
        serverConnector.addStateChangeHandler(event -> Scheduler.get()
                .scheduleDeferred(this::updateCountElementVisibility));
        textFieldConnector = (AbstractTextFieldConnector) serverConnector;
        textField = (VTextField) textFieldConnector.getWidget();
        var textFieldStyle = CLASSNAME + "-field";
        textField.addStyleName(textFieldStyle);

        wrapper = DOM.createDiv();
        var wrapperStyle = CLASSNAME + "-wrapper";
        wrapper.addClassName(wrapperStyle);

        countElement = DOM.createDiv();
        var characterCountStyle = CLASSNAME + "-count";
        countElement.addClassName(characterCountStyle);

        textField.addAttachHandler(this);
        textField.addKeyUpHandler(this);
        textField.addBlurHandler(event -> countElement.getStyle()
                .setDisplay(Style.Display.NONE));
    }

    private void updateCountElementVisibility() {
        if (textField.getValue().isEmpty()
                || textField.getStyleName().contains("v-textfield-prompt")) {
            countElement.getStyle().setDisplay(Style.Display.NONE);
        } else {
            countElement.getStyle().clearDisplay();
        }
        wrapper.getStyle().setProperty(WIDTH,
                textField.getElement().getStyle().getProperty(WIDTH));
    }

    @Override
    public void onKeyUp(KeyUpEvent keyUpEvent) {
        var maxChars = textFieldConnector.getState().maxLength;
        if (maxChars > 0) {
            int chars = maxChars - textField.getValue().length();
            // GWT does not support String formatter
            countElement.setInnerText(chars + " / " + maxChars);
        }
        updateCountElementVisibility();
    }

    @Override
    public void onAttachOrDetach(AttachEvent attachEvent) {
        if (attachEvent.isAttached()) {
            textField.getElement().getParentElement().appendChild(wrapper);
            textField.getElement().removeFromParent();
            wrapper.appendChild(textField.getElement());
            wrapper.appendChild(countElement);
            wrapper.getStyle().setProperty(WIDTH,
                    textField.getElement().getStyle().getProperty(WIDTH));
            countElement.getStyle().setProperty(BACKGROUND,
                    textField.getElement().getStyle().getProperty(BACKGROUND));
            updateCountElementVisibility();
        } else {
            var parentElement = wrapper.getParentElement();
            if (parentElement != null) {
                wrapper.removeChild(countElement);
                wrapper.removeChild(textField.getElement());
                parentElement.appendChild(textField.getElement());
            }
        }
    }
}
