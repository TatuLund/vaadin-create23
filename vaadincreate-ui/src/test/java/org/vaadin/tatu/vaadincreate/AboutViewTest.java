package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AboutView.MessageEvent;
import org.vaadin.tatu.vaadincreate.AppLayout.MenuButton;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

public class AboutViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private AboutView view;
    private String route = "";

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(route, AboutView.class);
        if (route.isEmpty()) {
            route = AboutView.VIEW_NAME;
        } else {
            route = "";
        }

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
        test($(MenuButton.class).id("about")).focus();
        // Changing field value should hide it
        assertFalse(area.isVisible());
        // Assert the new value
        note = $(Label.class).id("admins-note");
        assertEquals("<b><img>A new message</b>", note.getValue());

        waitWhile(view, n -> $(Notification.class).last() == null, 2);
        var notification = $(Notification.class).last();
        assertEquals("<b><img>A new message</b>",
                notification.getDescription());
        assertEquals(note.getCaption(), notification.getCaption());

        // Return the old value
        test($(Button.class).id("admin-edit")).click();
        assertTrue(area.isVisible());
        test(area).setValue(oldText);
        test($(MenuButton.class).id("about")).focus();
        assertFalse(area.isVisible());
    }

    @Test
    public void messageFromEventShown() {
        test($(Button.class).id("admin-edit")).click();
        var area = $(TextArea.class).id("admins-text-area");
        assertTrue(area.isVisible());

        EventBus.get().post(new MessageEvent("Hello", LocalDateTime.now()));

        waitWhile(view, n -> $(Notification.class).last() == null, 2);
        var notification = $(Notification.class).last();

        assertFalse(area.isVisible());

        assertEquals("Hello", notification.getDescription());
        var note = $(Label.class).id("admins-note");
        assertEquals("Hello", note.getValue());
        assertEquals(note.getCaption(), notification.getCaption());
    }

}
