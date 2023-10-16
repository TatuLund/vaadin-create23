package org.vaadin.tatu.vaadincreate;

import org.junit.Assert;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.ConfirmDialog.Type;

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
}
