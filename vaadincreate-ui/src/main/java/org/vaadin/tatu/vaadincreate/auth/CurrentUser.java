package org.vaadin.tatu.vaadincreate.auth;

import java.util.Optional;

import org.vaadin.tatu.vaadincreate.backend.data.User;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;

/**
 * Class for retrieving and setting the name of the current user of the current
 * session (without using JAAS). All methods of this class require that a
 * {@link VaadinRequest} is bound to the current thread.
 *
 *
 * @see com.vaadin.server.VaadinService#getCurrentRequest()
 */
public final class CurrentUser {

    /**
     * The attribute key used to store the username in the session.
     */
    public static final String CURRENT_USER_SESSION_ATTRIBUTE_KEY = CurrentUser.class
            .getCanonicalName();

    private CurrentUser() {
    }

    /**
     * Returns the name of the current user stored in the current session, or an
     * empty string if no user name is stored.
     *
     * @throws IllegalStateException
     *             if the current session cannot be accessed.
     */
    public static Optional<User> get() {
        var currentUser = (User) VaadinSession.getCurrent().getSession()
                .getAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY);
        if (currentUser == null) {
            return Optional.empty();
        } else {
            return Optional.of(currentUser);
        }
    }

    /**
     * Sets the name of the current user and stores it in the current session.
     * Using a {@code null} username will remove the username from the session.
     *
     * @throws IllegalStateException
     *             if the current session cannot be accessed.
     */
    public static void set(User currentUser) {
        if (currentUser == null) {
            VaadinSession.getCurrent().getSession()
                    .setAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY, null);
        } else {
            VaadinSession.getCurrent().getSession().setAttribute(
                    CURRENT_USER_SESSION_ATTRIBUTE_KEY, currentUser);
        }
    }
}