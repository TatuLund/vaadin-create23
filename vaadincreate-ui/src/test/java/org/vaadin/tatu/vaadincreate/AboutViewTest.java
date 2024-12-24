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

        $(view, VerticalLayout.class).first();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void when_entering_message_with_offending_content_it_is_being_sanitized() {
        var note = $(Label.class).id("admins-note");
        var oldText = note.getValue();

        // WHEN: Clicking edit button
        test($(Button.class).id("admin-edit")).click();

        // THEN: Text area is shown
        var area = $(TextArea.class).id("admins-text-area");
        assertTrue(area.isVisible());

        // WHEN: Changing the value that contains XSS and clicking outside
        test(area).setValue(
                "<b><img src=1 onerror=alert(document.domain)>A new message</b>");
        test($(MenuButton.class).id("about")).focus();

        // THEN: Text area is hidden and label is updated with sanitized value
        assertFalse(area.isVisible());
        note = $(Label.class).id("admins-note");
        assertEquals("<b><img>A new message</b>", note.getValue());

        // THEN: A notification is shown with sanitized value
        waitWhile(view, n -> $(Notification.class).last() == null, 2);
        var notification = $(Notification.class).last();
        assertEquals("<b><img>A new message</b>",
                notification.getDescription());
        assertEquals(note.getCaption(), notification.getCaption());

        // Return the old value for cleanup
        test($(Button.class).id("admin-edit")).click();
        assertTrue(area.isVisible());
        test(area).setValue(oldText);
        test($(MenuButton.class).id("about")).focus();
        assertFalse(area.isVisible());
    }

    @Test
    public void when_posting_a_new_message_event_it_is_shown_in_notification() {
        // WHEN: Clicking edit button
        test($(Button.class).id("admin-edit")).click();

        // THEN: Text area is shown
        var area = $(TextArea.class).id("admins-text-area");
        assertTrue(area.isVisible());

        // WHEN: Posting a new message event
        EventBus.get().post(new MessageEvent("Hello", LocalDateTime.now()));

        // THEN: Text area is hidden, a notification is shown and the message is
        // in the label
        waitWhile(view, n -> $(Notification.class).last() == null, 2);
        var notification = $(Notification.class).last();

        assertFalse(area.isVisible());

        assertEquals("Hello", notification.getDescription());
        var note = $(Label.class).id("admins-note");
        assertEquals("Hello", note.getValue());
        assertEquals(note.getCaption(), notification.getCaption());
    }

}
