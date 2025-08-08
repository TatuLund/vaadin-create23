package org.vaadin.tatu.vaadincreate.auth;

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
@NullMarked
public final class CurrentUser {

    /**
     * The attribute key used to store the username in the session.
     */
    public static final String CURRENT_USER_SESSION_ATTRIBUTE_KEY = CurrentUser.class
            .getCanonicalName();

    private CurrentUser() {
    }

    /**
     * Returns the current {@link User} stored in the current session, or
     * {@link Optional#empty()} if no user is stored.
     *
     * @throws IllegalStateException
     *             if the current session cannot be accessed.
     */
    public static Optional<User> get() {
        Objects.requireNonNull(VaadinSession.getCurrent(),
                "No Vaadin session is bound to the current thread");
        var session = VaadinSession.getCurrent().getSession();
        if (session == null) {
            return Optional.empty();
        }
        var currentUser = (User) session
                .getAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY);
        return Optional.ofNullable(currentUser);
    }

    /**
     * Stores the current {@link User} instance in the current session. Using a
     * {@code null} value will remove the user from the session.
     *
     * @throws IllegalStateException
     *             if the current session cannot be accessed.
     */
    public static void set(@Nullable User currentUser) {
        Objects.requireNonNull(VaadinSession.getCurrent(),
                "No Vaadin session is bound to the current thread");
        VaadinSession.getCurrent().getSession()
                .setAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY, currentUser);
    }
}