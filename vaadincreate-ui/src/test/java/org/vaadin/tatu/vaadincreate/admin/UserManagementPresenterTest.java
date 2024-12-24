package org.vaadin.tatu.vaadincreate.admin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;

import com.vaadin.server.ServiceException;

public class UserManagementPresenterTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private UserManagementPresenter presenter;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("User1", "user1");

        presenter = new UserManagementPresenter(new UserManagementView());
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test(expected = IllegalStateException.class)
    public void cantFetchUsers() {
        presenter.requestUpdateUsers();
    }

    @Test(expected = IllegalStateException.class)
    public void cantRemoveUsers() {
        presenter.removeUser(123);
    }

    @Test(expected = IllegalStateException.class)
    public void cantUpdateUser() {
        presenter.updateUser(null);
    }
}
