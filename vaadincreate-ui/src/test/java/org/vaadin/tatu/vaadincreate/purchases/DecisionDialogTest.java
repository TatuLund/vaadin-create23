package org.vaadin.tatu.vaadincreate.purchases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;

import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.UIUnitTest;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;

/**
 * UI unit tests for DecisionWindow behavior in approve and reject modes.
 */
public class DecisionDialogTest extends UIUnitTest {

    private UI ui;

    @Before
    public void setup() throws ServiceException {
        ui = mockVaadin();
        ui.getSession().setLocale(DefaultI18NProvider.LOCALE_EN);
    }

    @After
    public void cleanUp() {
        tearDown();
    }

    @Test
    public void reject_mode_confirm_button_is_disabled_until_non_blank_reason_is_entered() {
        var window = new DecisionDialog(false, comment -> {
            // No-op for button state test.
        });
        window.open();

        var confirmButton = $(Button.class)
                .id(DecisionDialog.CONFIRM_BUTTON_ID);
        var commentField = $(TextArea.class)
                .id(DecisionDialog.DECISION_COMMENT_ID);

        assertNotNull(confirmButton);
        assertNotNull(commentField);
        assertFalse(
                "Reject confirm button must start disabled until reason is entered",
                confirmButton.isEnabled());

        test(commentField).setValue("   ");
        assertFalse("Whitespace-only reason must keep confirm disabled",
                confirmButton.isEnabled());

        test(commentField).setValue("Valid reason");
        assertTrue("Non-blank reason must enable confirm button",
                confirmButton.isEnabled());
    }

    @Test
    public void reject_mode_confirm_trims_reason_closes_window_and_marks_confirmed() {
        var callbackComment = new AtomicReference<String>();
        var window = new DecisionDialog(false, callbackComment::set);
        window.open();

        var commentField = $(TextArea.class)
                .id(DecisionDialog.DECISION_COMMENT_ID);
        var confirmButton = $(Button.class)
                .id(DecisionDialog.CONFIRM_BUTTON_ID);

        test(commentField).setValue("  Budget exceeded  ");
        test(confirmButton).click();

        assertTrue("Window should be marked confirmed after confirm click",
                window.isConfirmed());
        assertFalse("Window should be closed after confirm click",
                window.isAttached());
        assertEquals("Budget exceeded", callbackComment.get());
    }

    @Test
    public void reject_mode_cancel_closes_window_without_callback_and_without_confirmed_state() {
        var callbackCalled = new AtomicBoolean(false);
        var window = new DecisionDialog(false,
                comment -> callbackCalled.set(true));
        window.open();

        var cancelButton = $(Button.class)
                .id(DecisionDialog.CANCEL_BUTTON_ID);
        test(cancelButton).click();

        assertFalse("Cancel must not mark window as confirmed",
                window.isConfirmed());
        assertFalse("Window should be closed after cancel click",
                window.isAttached());
        assertFalse("Cancel must not invoke decision callback",
                callbackCalled.get());
    }

    @Test
    public void approve_mode_confirm_is_enabled_and_blank_comment_maps_to_null() {
        var callbackComment = new AtomicReference<String>();
        var window = new DecisionDialog(true, callbackComment::set);
        window.open();

        var confirmButton = $(Button.class)
                .id(DecisionDialog.CONFIRM_BUTTON_ID);
        assertTrue("Approve confirm button should be enabled immediately",
                confirmButton.isEnabled());

        test(confirmButton).click();

        assertTrue("Window should be marked confirmed after confirm click",
                window.isConfirmed());
        assertFalse("Window should be closed after confirm click",
                window.isAttached());
        assertNull("Blank approve comment should be passed as null",
                callbackComment.get());
    }

    @Test
    public void approve_mode_confirm_trims_non_blank_comment_before_callback() {
        var callbackComment = new AtomicReference<String>();
        var window = new DecisionDialog(true, callbackComment::set);
        window.open();

        var commentField = $(TextArea.class)
                .id(DecisionDialog.DECISION_COMMENT_ID);
        var confirmButton = $(Button.class)
                .id(DecisionDialog.CONFIRM_BUTTON_ID);

        test(commentField).setValue("  Looks good  ");
        test(confirmButton).click();

        assertEquals("Looks good", callbackComment.get());
    }
}
