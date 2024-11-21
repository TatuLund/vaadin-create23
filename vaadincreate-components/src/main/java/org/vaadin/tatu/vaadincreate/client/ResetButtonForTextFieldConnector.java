package org.vaadin.tatu.vaadincreate.client;

import org.vaadin.tatu.vaadincreate.ResetButtonForTextField;
import org.vaadin.tatu.vaadincreate.shared.ResetButtonForTextFieldState;

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

/**
 * Client side implementation of {@link ResetButtonForTextField}.
 * 
 * @see <a href="https://vaadin.com/blog/-/blogs/2656782">Extending components
 *      in Vaadin 7</a>
 */
@SuppressWarnings({ "serial", "java:S1948" })
@Connect(ResetButtonForTextField.class)
public class ResetButtonForTextFieldConnector extends AbstractExtensionConnector
        implements KeyUpHandler, AttachEvent.Handler {

    public static final String CLASSNAME = "resetbuttonfortextfield";
    private AbstractTextFieldConnector textFieldConnector;
    private VTextField textField;
    private Element resetButtonElement;

    @Override
    protected void extend(ServerConnector serverConnector) {
        serverConnector.addStateChangeHandler(event -> Scheduler.get()
                .scheduleDeferred(this::updateResetButtonVisibility));
        textFieldConnector = (AbstractTextFieldConnector) serverConnector;
        textField = (VTextField) textFieldConnector.getWidget();
        var textFieldStyle = CLASSNAME + "-textfield";
        textField.addStyleName(textFieldStyle);

        resetButtonElement = DOM.createDiv();
        var resetButtonStyle = CLASSNAME + "-resetbutton";
        resetButtonElement.addClassName(resetButtonStyle);
        resetButtonElement.setAttribute("role", "button");
        resetButtonElement.setAttribute("tabindex", "0");
        resetButtonElement.setAttribute("aria-label", getState().buttonLabel);

        textField.addAttachHandler(this);
        textField.addKeyUpHandler(this);
    }

    private void updateResetButtonVisibility() {
        if (textField.getValue().isEmpty()
                || textField.getStyleName().contains("v-textfield-prompt")) {
            resetButtonElement.getStyle().setDisplay(Style.Display.NONE);
        } else {
            resetButtonElement.getStyle().clearDisplay();
        }
    }

    /**
     * Adds a click listener to the specified element that triggers the clearing
     * of a text field.
     * 
     * @param el
     *            The element to attach the click listener to.
     */
    public native void addResetButtonClickListener(Element el)
    /*-{
    var self = this;
    el.onclick = $entry(function() {
      self.@org.vaadin.tatu.vaadincreate.client.ResetButtonForTextFieldConnector::clearTextField()();
    });
    }-*/;

    public native void removeResetButtonClickListener(Element el)
    /*-{
    el.onclick = null;
    }-*/;

    @Override
    public void onKeyUp(KeyUpEvent keyUpEvent) {
        updateResetButtonVisibility();
    }

    @Override
    public void onAttachOrDetach(AttachEvent attachEvent) {
        if (attachEvent.isAttached()) {
            textField.getElement().getParentElement()
                    .insertAfter(resetButtonElement, textField.getElement());
            updateResetButtonVisibility();
            addResetButtonClickListener(resetButtonElement);
        } else {
            var parentElement = resetButtonElement.getParentElement();
            if (parentElement != null) {
                parentElement.removeChild(resetButtonElement);
            }
            removeResetButtonClickListener(resetButtonElement);
        }
    }

    // This method is called from JSNI
    @SuppressWarnings("java:S1144")
    private void clearTextField() {
        textField.setValue("");
        textFieldConnector.flush();
        updateResetButtonVisibility();
        textField.getElement().focus();
    }

    @Override
    public ResetButtonForTextFieldState getState() {
        return (ResetButtonForTextFieldState) super.getState();
    }

}