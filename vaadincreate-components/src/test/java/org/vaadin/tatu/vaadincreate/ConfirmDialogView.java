package org.vaadin.tatu.vaadincreate;

import com.vaadin.navigator.View;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

public class ConfirmDialogView extends VerticalLayout implements View {
    public static final String NAME = "confirm-dialog";

    public ConfirmDialogView() {
        var dialog = new ConfirmDialog("Are you sure?",
                ConfirmDialog.Type.ALERT);
        dialog.addConfirmedListener(e -> {
            Notification.show("Confirmed");
        });
        dialog.addCancelListener(e -> {
            Notification.show("Cancelled");
        });
        var button = new Button("Open dialog", e -> dialog.open());
        addComponent(button);
    }
}
