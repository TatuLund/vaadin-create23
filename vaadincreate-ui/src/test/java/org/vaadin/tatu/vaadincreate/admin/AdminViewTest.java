package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.ErrorView;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;

import com.vaadin.ui.Label;

import com.vaadin.server.ServiceException;

public class AdminViewTest extends AbstractUITest {

    @Before
    public void setup() throws ServiceException {
        mockVaadin(new VaadinCreateUI());
        login();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void navigating_to_unknown_admin_subview_shows_ErrorView_and_preserves_fragment() {
        // WHEN: Navigating directly to an unknown admin subview
        var ui = VaadinCreateUI.get();
        ui.getNavigator().navigateTo(AdminView.VIEW_NAME + "/test");

        // THEN: Global ErrorView is shown
        var view = ui.getNavigator().getCurrentView();
        assertNotNull(view);
        assertEquals(ErrorView.class, view.getClass());

        // AND: The attempted path is shown in the message
        var explanation = $(Label.class).id("explanation");
        assertEquals(
                "You tried to navigate to a view \"admin/test\" that does not exist.",
                explanation.getValue());

        // AND: The browser fragment remains the original attempted path
        assertEquals("!admin/test", ui.getPage().getUriFragment());
    }
}
