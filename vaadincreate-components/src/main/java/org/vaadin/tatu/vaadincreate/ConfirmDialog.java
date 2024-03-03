package org.vaadin.tatu.vaadincreate;

import java.lang.reflect.Method;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.ReflectTools;

@SuppressWarnings("serial")
public class ConfirmDialog extends Window {

    /**
     * Type of the ConfirmDialog
     */
    public enum Type {
        // @formatter:off
        /**
         * Success colored dialog
         */
        SUCCESS, 
        /**
         * Alert, warning style dialog
         */
        ALERT, 
        /**
         * Default style dialog
         */
        BLANK;
        // @formatter:on
    }

    /**
     * Constructs a ConfirmDialog with given text and Type for style:
     * 
     * @see Type
     * 
     * @param text
     *            Text shown in Dialog body
     * @param type
     *            Type of the Dialog
     */
    public ConfirmDialog(String text, Type type) {
        setId("confirm-dialog");
        setModal(true);
        setClosable(false);
        setResizable(false);
        setWidth("50%");
        setHeight("50%");
        setDraggable(false);
        setIcon(VaadinIcons.EXCLAMATION_CIRCLE);
        var message = new Label(text);
        message.setSizeFull();
        if (type == Type.SUCCESS) {
            message.addStyleName(ValoTheme.LABEL_SUCCESS);
        } else if (type == Type.ALERT) {
            message.addStyleName(ValoTheme.LABEL_FAILURE);
        }
        var content = new VerticalLayout();
        content.setSizeFull();
        var cancelButton = new Button("Cancel", e -> close());
        cancelButton.setId("cancel-button");
        var confirmButton = new Button("Confirm", e -> {
            this.fireEvent(new ConfirmedEvent(this));
            close();
        });
        confirmButton.setId("confirm-button");
        confirmButton.setClickShortcut(KeyCode.ENTER);
        confirmButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        var buttons = new HorizontalLayout();
        buttons.addComponents(cancelButton, confirmButton);
        buttons.setComponentAlignment(cancelButton, Alignment.BOTTOM_LEFT);
        buttons.setComponentAlignment(confirmButton, Alignment.BOTTOM_RIGHT);
        buttons.setWidthFull();
        content.addComponents(message, buttons);
        content.setExpandRatio(message, 1);
        content.setComponentAlignment(message, Alignment.MIDDLE_CENTER);
        content.setComponentAlignment(buttons, Alignment.BOTTOM_CENTER);
        setContent(content);
    }

    /**
     * Add event listener for confirmed event. Event is fired when user clicks
     * confirm button.
     * 
     * @param listener
     *            The listener, can be Lambda expression.
     * @return Registration Use Registration#remove() for listener removal.
     */
    public Registration addConfirmedListener(ConfirmedListener listener) {
        return addListener(ConfirmedEvent.class, listener,
                ConfirmedListener.MEDIA_PAUSED_METHOD);
    }

    /**
     * Confirm listener interface, can be implemented with Lambda or anonymous
     * inner class.
     */
    public interface ConfirmedListener extends ConnectorEventListener {
        Method MEDIA_PAUSED_METHOD = ReflectTools.findMethod(
                ConfirmedListener.class, "confirmed", ConfirmedEvent.class);

        public void confirmed(ConfirmedEvent event);
    }

    /**
     * ConfirmEvent is fired when user clicks confirm button of the
     * ConfirmDialog.
     */
    public static class ConfirmedEvent extends Component.Event {

        public ConfirmedEvent(Component source) {
            super(source);
        }
    }
}
