package org.vaadin.tatu.vaadincreate.client;

import org.vaadin.tatu.vaadincreate.CapsLockWarning;
import org.vaadin.tatu.vaadincreate.shared.CapsLockWarningState;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.shared.ui.Connect;

@SuppressWarnings({ "serial", "deprecation", "java:S1948" })
@Connect(CapsLockWarning.class)
public class CapsLockWarningConnector extends AbstractExtensionConnector {

    private HTML messageElement;
    private VOverlay warning;
    private Widget passwordWidget;

    @Override
    protected void extend(ServerConnector target) {
        passwordWidget = ((ComponentConnector) target).getWidget();

        warning = new VOverlay();
        warning.setStyleName("v-tooltip capslock");
        warning.setOwner(passwordWidget);
        messageElement = new HTML(getState().message);
        messageElement.setStyleName("capslock-message");
        messageElement.getElement().setAttribute("aria-live", "polite");
        warning.add(messageElement);
        addKeyDownListener(passwordWidget.getElement());
    }

    public void showWarning() {
        warning.showRelativeTo(passwordWidget);
    }

    public void hideWarning() {
        warning.hide();
    }

    // This is native JavaScript function. GWT does not have this new API.
    private native void addKeyDownListener(Element el)
    /*-{
    var self = this;
    el
        .addEventListener(
            'keydown',
            $entry(function(e) {
              if (e.getModifierState('CapsLock')) {
                self.@org.vaadin.tatu.vaadincreate.client.CapsLockWarningConnector::showWarning()();
              } else {
                self.@org.vaadin.tatu.vaadincreate.client.CapsLockWarningConnector::hideWarning()();
              }
            }));
    }-*/;

    @OnStateChange("message")
    void setMessage() {
        messageElement.setHTML(getState().message);
    }

    @Override
    public CapsLockWarningState getState() {
        return (CapsLockWarningState) super.getState();
    }
}
