package org.vaadin.tatu.vaadincreate;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.ConfirmDialog.Type;

import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class ConfirmDialogTest {

    @Test
    public void dialogComposition() {
        var dialog = new ConfirmDialog("Confirm", "Are you sure?", Type.ALERT);
        Assert.assertFalse(dialog.window.isDraggable());
        Assert.assertTrue(dialog.window.isModal());
        Assert.assertFalse(dialog.window.isResizable());
        var content = (VerticalLayout) dialog.window.getContent();
        var message = (Label) content.getComponent(0);
        Assert.assertEquals("Are you sure?", message.getValue());
    }

    @Test
    public void confirmEvent() {
        var dialog = new ConfirmDialog("Confirm", "Are you sure?", Type.ALERT);

        var content = (VerticalLayout) dialog.window.getContent();
        var buttons = (HorizontalLayout) content.getComponent(1);
        var confirmButton = (Button) buttons.getComponent(1);
        var count = new AtomicInteger(0);
        dialog.addConfirmedListener(e -> {
            count.addAndGet(1);
        });
        confirmButton.click();
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void cancelEvent() {
        var dialog = new ConfirmDialog("Confirm", "Are you sure?", Type.ALERT);

        var content = (VerticalLayout) dialog.window.getContent();
        var buttons = (HorizontalLayout) content.getComponent(1);
        var cancelButton = (Button) buttons.getComponent(0);
        var count = new AtomicInteger(0);
        dialog.addCancelListener(e -> {
            count.addAndGet(1);
        });
        cancelButton.click();
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void testButtonTexts() {
        var dialog = new ConfirmDialog("Confirm", "Are you sure?", Type.ALERT);

        dialog.setConfirmText("Yes");
        dialog.setCancelText("No");

        var content = (VerticalLayout) dialog.window.getContent();
        var buttons = (HorizontalLayout) content.getComponent(1);

        var cancelButton = (Button) buttons.getComponent(0);
        Assert.assertEquals("No", cancelButton.getCaption());

        var confirmButton = (Button) buttons.getComponent(1);
        Assert.assertEquals("Yes", confirmButton.getCaption());
    }
}
