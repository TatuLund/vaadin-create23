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

    public enum Type {
        SUCCESS, ALERT, BLANK;
    }

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

    public Registration addConfirmedListener(ConfirmedListener listener) {
        return addListener(ConfirmedEvent.class, listener,
                ConfirmedListener.MEDIA_PAUSED_METHOD);
    }

    public interface ConfirmedListener extends ConnectorEventListener {
        Method MEDIA_PAUSED_METHOD = ReflectTools.findMethod(
                ConfirmedListener.class, "confirmed", ConfirmedEvent.class);

        public void confirmed(ConfirmedEvent event);
    }

    public static class ConfirmedEvent extends Component.Event {

        public ConfirmedEvent(Component source) {
            super(source);
        }
    }
}
