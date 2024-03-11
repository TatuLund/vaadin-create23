package org.vaadin.tatu.vaadincreate.login;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.MockVaadinSession;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import com.vaadin.server.VaadinSession;

public class LoginViewTest {

    @Test
    public void loginEvent() {
        var session = new MockVaadinSession(null);
        VaadinSession.setCurrent(session);
        session.lock();

        var count = new AtomicInteger(0);
        var login = new LoginView(new MockAccessControl("Admin"),
                e -> count.addAndGet(1));

        login.buildUI();

        login.username.setValue("Admin");
        login.password.setValue("Admin");
        login.login.click();
        Assert.assertEquals(1, count.get());
    }

    @SuppressWarnings("serial")
    public static class MockAccessControl implements AccessControl {

        private boolean signedIn = false;
        private String principal;

        public MockAccessControl(String principal) {
            this.principal = principal;
        }

        @Override
        public boolean signIn(String username, String password) {
            signedIn = username.equals(principal) && username.equals(password);
            return signedIn;
        }

        @Override
        public boolean isUserSignedIn() {
            return signedIn;
        }

        @Override
        public boolean isUserInRole(Role role) {
            return (role == Role.ADMIN && principal.equals("Admin"))
                    || role == Role.USER;
        }

        @Override
        public String getPrincipalName() {
            return principal;
        }

    }
}
