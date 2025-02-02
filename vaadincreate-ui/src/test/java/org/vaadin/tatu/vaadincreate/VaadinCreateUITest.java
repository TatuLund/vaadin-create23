package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AppLayout.MenuButton;
import org.vaadin.tatu.vaadincreate.admin.AdminView;
import org.vaadin.tatu.vaadincreate.admin.UserManagementPresenter.UserUpdatedEvent;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.crud.BookGrid;
import org.vaadin.tatu.vaadincreate.crud.BooksView;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

import com.vaadin.server.ServiceException;

public class VaadinCreateUITest extends AbstractUITest {

    private VaadinCreateUI ui;
    private String route = "";

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("Super", "super");

        navigate(route, AboutView.class);
        if (route.isEmpty()) {
            route = AboutView.VIEW_NAME;
        } else {
            route = "";
        }
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void when_user_update_event_is_observed_current_user_will_be_updated() {
        var user = CurrentUser.get().get();
        var oldName = user.getName();
        user.setRole(Role.USER);
        var newName = "New Name";
        user.setName(newName);

        // WHEN: User is updated
        var newUser = ui.getUserService().updateUser(user);
        EventBus.get().post(new UserUpdatedEvent(newUser));

        test($(MenuButton.class).id("inventory")).click();
        var view = $(BooksView.class).first();
        var grid = $(BookGrid.class).first();
        waitForGrid(view, grid);

        // THEN: Current user is updated
        assertSame(newUser, CurrentUser.get().get());
        assertEquals(newName, CurrentUser.get().get().getName());
        assertEquals(Role.USER, CurrentUser.get().get().getRole());

        // THEN: User can't edit books
        test(grid).click(1, 0);
        assertFalse($(BookForm.class).first().isShown());

        // THEN: User can't access admin view
        test($(MenuButton.class).id("admin")).click();
        var adminView = $(AdminView.class).first();
        assertNull(adminView);

        // Cleanup
        newUser.setName(oldName);
        newUser.setRole(Role.ADMIN);
        ui.getUserService().updateUser(newUser);
    }
}
