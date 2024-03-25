package org.vaadin.tatu.vaadincreate.login;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.auth.MockAccessControl;
import org.vaadin.tatu.vaadincreate.uiunittest.UIUnitTest;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.UI;

public class LoginViewTest extends UIUnitTest {

    @Test
    public void loginEventFired() throws ServiceException {
        var ui = mockVaadin();

        var count = new AtomicInteger(0);
        var login = new LoginView(new MockAccessControl("Admin"),
                e -> count.addAndGet(1));
        ui.setContent(login);

        login.username.setValue("Admin");
        login.password.setValue("Admin");
        login.login.click();
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void loginEventNotFired() throws ServiceException {
        var ui = mockVaadin();

        var count = new AtomicInteger(0);
        var login = new LoginView(new MockAccessControl("Admin"),
                e -> count.addAndGet(1));
        ui.setContent(login);

        login.username.setValue("Admin");
        login.password.setValue("Wrong");
        login.login.click();
        Assert.assertEquals(0, count.get());
    }}
