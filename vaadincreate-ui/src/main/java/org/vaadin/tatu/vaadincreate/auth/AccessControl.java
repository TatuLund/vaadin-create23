package org.vaadin.tatu.vaadincreate.auth;

import java.io.Serializable;

import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

/**
 * Simple interface for authentication and authorization checks.
 */
public interface AccessControl extends Serializable {

    /**
     * Sign in the user.
     * 
     * @param username
     *            Username as String
     * @param password
     *            Password as String
     * @return boolean value, false if sign in failed.
     */
    public boolean signIn(String username, String password);

    /**
     * 
     * @return boolean value, true if there is user signed in the session
     */
    public boolean isUserSignedIn();

    /**
     * Test if the user has given role.
     * 
     * @param role
     *            Role to be tested.
     * @return boolean value, true if the user has the tested role.
     */
    public boolean isUserInRole(Role role);

    /**
     * Get the name of currently signed in user in the session.
     * 
     * @return String value
     */
    public String getPrincipalName();
}
