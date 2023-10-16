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
        var dialog = new ConfirmDialog("Are you sure?", Type.ALERT);
        Assert.assertFalse(dialog.isDraggable());
        Assert.assertTrue(dialog.isModal());
        Assert.assertFalse(dialog.isResizable());
        var content = (VerticalLayout) dialog.getContent();
        var message = (Label) content.getComponent(0);
        Assert.assertEquals("Are you sure?", message.getValue());
    }

    @Test
    public void confirmEvent() {
        var dialog = new ConfirmDialog("Are you sure?", Type.ALERT);

        var content = (VerticalLayout) dialog.getContent();
        var buttons = (HorizontalLayout) content.getComponent(1);
        var confirmButton = (Button) buttons.getComponent(1);
        var count = new AtomicInteger(0);
        dialog.addConfirmedListener(e -> {
            count.addAndGet(1);
        });
        confirmButton.click();
        Assert.assertEquals(1, count.get());
    }
}
