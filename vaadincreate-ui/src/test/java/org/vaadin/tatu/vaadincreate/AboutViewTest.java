package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.vaadin.tatu.vaadincreate.crud.BookForm;
import org.vaadin.tatu.vaadincreate.crud.BookGrid;
import org.vaadin.tatu.vaadincreate.crud.BooksView;

import com.vaadin.server.ServiceException;
import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.LabelElement;
import com.vaadin.testbench.elements.TextAreaElement;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

public class AboutViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private AboutView view;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(AboutView.VIEW_NAME, AboutView.class);

        var layout = $(view, VerticalLayout.class).first();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void leaveMessageSanitized() {
        var note = $(Label.class).id("admins-note");
        var oldText = note.getValue();

        test($(Button.class).id("admin-edit")).click();
        var area = $(TextArea.class).id("admins-text-area");
        assertTrue(area.isVisible());
        // If not sanitized this would be XSS
        test(area).setValue(
                "<b><img src=1 onerror=alert(document.domain)>A new message</b>");
        test($(Button.class).id("about")).focus();
        // Changing field value should hide it
        assertFalse(area.isVisible());
        // Assert the new value
        note = $(Label.class).id("admins-note");
        assertEquals("<b><img>A new message</b>", note.getValue());

        waitWhile(view, n -> $(Notification.class).last() == null, 2);
        var notification = $(Notification.class).last();
        assertEquals("<b><img>A new message</b>",
                notification.getDescription());

        // Return the old value
        test($(Button.class).id("admin-edit")).click();
        assertTrue(area.isVisible());
        test(area).setValue(oldText);
        test($(Button.class).id("about")).focus();
        assertFalse(area.isVisible());
    }

}
