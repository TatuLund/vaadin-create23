package org.vaadin.tatu.vaadincreate.components;

import java.lang.reflect.Method;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.window.WindowRole;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.ReflectTools;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class ConfirmDialog extends Composite {

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

    private Button cancelButton;
    private Button confirmButton;
    Window window = new Window();

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
    public ConfirmDialog(String caption, @Nullable String text, Type type) {
        window.setId("confirm-dialog");
        window.setModal(true);
        window.setClosable(false);
        window.setResizable(false);
        window.setWidth("50%");
        window.setHeight("50%");
        window.setDraggable(false);
        window.setIcon(VaadinIcons.EXCLAMATION_CIRCLE);
        window.setCaption(caption);
        var message = new Label(text);
        message.setSizeFull();
        if (type == Type.SUCCESS) {
            message.addStyleName(ValoTheme.LABEL_SUCCESS);
            window.setAssistiveRole(WindowRole.DIALOG);
        } else if (type == Type.ALERT) {
            message.addStyleName(ValoTheme.LABEL_FAILURE);
            window.setAssistiveRole(WindowRole.ALERTDIALOG);
        }
        window.setAssistiveDescription(message);
        var content = new VerticalLayout();
        content.setSizeFull();
        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(click -> {
            cancelButton.removeClickShortcut();
            fireEvent(new CancelledEvent(this));
            window.close();
        });
        cancelButton.setId("cancel-button");
        AttributeExtension.of(cancelButton)
                .setAttribute(AriaAttributes.KEYSHORTCUTS, "Escape");
        confirmButton = new Button("Confirm");
        confirmButton.addClickListener(click -> {
            fireEvent(new ConfirmedEvent(this));
            window.close();
        });
        confirmButton.setId("confirm-button");
        confirmButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        AttributeExtension.of(confirmButton)
                .setAttribute(AriaAttributes.KEYSHORTCUTS, "Enter");
        var buttons = new HorizontalLayout();
        buttons.addComponents(cancelButton, confirmButton);
        buttons.setComponentAlignment(cancelButton, Alignment.BOTTOM_LEFT);
        buttons.setComponentAlignment(confirmButton, Alignment.BOTTOM_RIGHT);
        buttons.setWidthFull();
        content.addComponents(message, buttons);
        content.setExpandRatio(message, 1);
        content.setComponentAlignment(message, Alignment.MIDDLE_CENTER);
        content.setComponentAlignment(buttons, Alignment.BOTTOM_CENTER);
        window.setContent(content);
    }

    /**
     * Set the caption of the Cancel button.
     * 
     * @param cancelText
     *            The caption string
     */
    public void setCancelText(@Nullable String cancelText) {
        cancelButton.setCaption(cancelText);
    }

    /**
     * Set the caption of the Confirm button.
     * 
     * @param confirmText
     *            The caption string
     */
    public void setConfirmText(@Nullable String confirmText) {
        confirmButton.setCaption(confirmText);
    }

    /**
     * Open the dialog
     */
    public void open() {
        UI.getCurrent().addWindow(window);
        Shortcuts.setEscapeShortcut("#confirm-dialog", "#cancel-button");
        confirmButton.setClickShortcut(KeyCode.ENTER);
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
                ConfirmedListener.CONFIRMED_METHOD);
    }

    /**
     * Confirm listener interface, can be implemented with Lambda or anonymous
     * inner class.
     */
    public interface ConfirmedListener extends ConnectorEventListener {
        Method CONFIRMED_METHOD = ReflectTools.findMethod(
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

    /**
     * Add event listener for cancelled event. Event is fired when user clicks
     * cancel button.
     * 
     * @param listener
     *            The listener, can be Lambda expression.
     * @return Registration Use Registration#remove() for listener removal.
     */
    public Registration addCancelledListener(CancelledListener listener) {
        return addListener(CancelledEvent.class, listener,
                CancelledListener.CANCELLED_METHOD);
    }

    /**
     * Cancel listener interface, can be implemented with Lambda or anonymous
     * inner class.
     */
    public interface CancelledListener extends ConnectorEventListener {
        Method CANCELLED_METHOD = ReflectTools.findMethod(
                CancelledListener.class, "cancelled", CancelledEvent.class);

        public void cancelled(CancelledEvent event);
    }

    /**
     * CancelEvent is fired when user clicks cancel button of the ConfirmDialog.
     */
    public static class CancelledEvent extends Component.Event {

        public CancelledEvent(Component source) {
            super(source);
        }
    }

}
