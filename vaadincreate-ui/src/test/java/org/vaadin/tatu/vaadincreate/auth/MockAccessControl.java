package org.vaadin.tatu.vaadincreate.auth;

import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

@SuppressWarnings("serial")
public class MockAccessControl implements AccessControl {

    private boolean signedIn = false;
    private String principal;

    public MockAccessControl(String principal) {
        this.principal = principal;
    }

    @Override
    public boolean signIn(String username, String password) {
        signedIn = username.equals(principal) && username.equals(password);
        CurrentUser.set(new User(1, principal, principal,
                principal.equals("Admin") ? Role.ADMIN : Role.USER));
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
