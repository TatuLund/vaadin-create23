package org.vaadin.tatu.vaadincreate.auth;

import java.io.Serializable;

import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

/**
 * Simple interface for authentication and authorization checks.
 */
public interface AccessControl extends Serializable {

    public boolean signIn(String username, String password);

    public boolean isUserSignedIn();

    public boolean isUserInRole(Role role);

    public String getPrincipalName();
}
