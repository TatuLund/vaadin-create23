package org.vaadin.tatu.vaadincreate.auth;

import java.io.Serializable;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

/**
 * Simple interface for authentication and authorization checks.
 */
@NullMarked
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
     * @throws IllegalStateException
     *             if no user is signed in
     */
    public String getPrincipalName();

    /**
     * Get AccessControl associated with the UI
     *
     * @return AccessControl
     */
    public static AccessControl get() {
        return VaadinCreateUI.get().getAccessControl();
    }

    /**
     * Utility method, which throws exception if the user is not in Admin role.
     */
    public default void assertAdmin() {
        if (!isUserInRole(Role.ADMIN)) {
            throw new IllegalStateException(
                    "Operation allowed only with ADMIN role.");
        }
    }
}
