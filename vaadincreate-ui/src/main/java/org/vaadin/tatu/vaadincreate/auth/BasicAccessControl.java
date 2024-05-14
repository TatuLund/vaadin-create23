package org.vaadin.tatu.vaadincreate.auth;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

/**
 * Default mock implementation of {@link AccessControl}. This implementation
 * uses UserService for user information (credentials and roles).
 */
@SuppressWarnings("serial")
public class BasicAccessControl implements AccessControl {

    @Override
    public boolean signIn(String username, String password) {
        Objects.requireNonNull(username, password);
        Optional<User> user = VaadinCreateUI.get().getUserService()
                .findByName(username);
        if (!user.isPresent()) {
            logger.warn("User '" + username + "' unknown");
            return false;
        } else {
            if (user.get().getPasswd().equals(password)) {
                CurrentUser.set(user.get());
                logger.info("User '" + username + "' logged in");
                return true;
            }
        }
        logger.warn("User '" + username + "' logging failed");
        return false;
    }

    @Override
    public boolean isUserSignedIn() {
        return !CurrentUser.get().isEmpty();
    }

    @Override
    public boolean isUserInRole(Role role) {
        assert (CurrentUser.get().isPresent());
        User user = CurrentUser.get().get();
        if (user.getRole() == role) {
            return true;
        }
        return false;
    }

    @Override
    public String getPrincipalName() {
        assert (CurrentUser.get().isPresent());
        User user = CurrentUser.get().get();
        return user.getName();
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

}