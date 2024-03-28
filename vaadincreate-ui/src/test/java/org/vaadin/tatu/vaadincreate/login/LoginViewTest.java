package org.vaadin.tatu.vaadincreate.login;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.auth.MockAccessControl;
import org.vaadin.tatu.vaadincreate.uiunittest.UIUnitTest;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.UI;

public class LoginViewTest extends UIUnitTest {

    UI ui;

    @Before
    public void setup() throws ServiceException {
        // Vaadin mocks
        ui = mockVaadin();
    }

    @After
    public void cleanup() {
        tearDown();
    }

    @Test
    public void loginEventFired() throws ServiceException {
        var count = new AtomicInteger(0);
        var login = new LoginView(new MockAccessControl("Admin"),
                e -> count.addAndGet(1));
        ui.setContent(login);

        test(login.username).setValue("Admin");
        test(login.password).setValue("Admin");
        test(login.login).click();
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void loginEventNotFired() throws ServiceException {
        var count = new AtomicInteger(0);
        var login = new LoginView(new MockAccessControl("Admin"),
                e -> count.addAndGet(1));
        ui.setContent(login);

        test(login.username).setValue("Admin");
        test(login.password).setValue("Wrong");
        test(login.login).click();
        Assert.assertEquals(0, count.get());
    }}
