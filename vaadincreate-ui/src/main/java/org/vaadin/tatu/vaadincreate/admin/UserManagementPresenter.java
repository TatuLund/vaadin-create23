package org.vaadin.tatu.vaadincreate.admin;

import java.io.Serializable;

import javax.persistence.OptimisticLockException;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

@NullMarked
@SuppressWarnings("serial")
public class UserManagementPresenter implements Serializable {

    private UserManagementView view;
    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    public UserManagementPresenter(UserManagementView view) {
        this.view = view;
    }

    /**
     * Requests an update of the user list.
     * 
     * This method asserts that the current user has admin privileges, logs the
     * action of fetching users, and updates the view with the list of all users
     * retrieved from the UserService.
     * 
     * @throws IllegalStateException
     *             if the current user is not an admin
     */
    public void requestUpdateUsers() {
        accessControl.assertAdmin();
        logger.info("Fetching users");
        view.setUsers(UserService.get().getAllUsers());
    }

    /**
     * Removes a user with the specified ID.
     * 
     * This method first checks if the current user has admin privileges. If the
     * user has the necessary privileges, it proceeds to remove the user with
     * the given ID from the system. After successfully removing the user, it
     * logs the action, notifies the view to show a user removed message, and
     * requests an update of the user list.
     * 
     * @param id
     *            the ID of the user to be removed
     * @throws IllegalStateException
     *             if the current user does not have admin privileges
     */
    public void removeUser(Integer id) {
        accessControl.assertAdmin();
        getService().removeUser(id);
        logger.info("User '{}' removed.", id);
        view.showUserRemoved();
        requestUpdateUsers();
    }

    /**
     * Updates the given user.
     * <p>
     * This method asserts that the current user has admin privileges before
     * attempting to update the user. If the update is successful, it posts a
     * {@link UserUpdatedEvent} to the event bus, shows a user updated message
     * in the view, requests an update of the users list, and logs the update.
     * <p>
     * If the user update fails due to a duplicate entry, it shows a duplicate
     * error message in the view. If the update fails due to an optimistic
     * locking conflict, it requests an update of the users list and shows a
     * save conflict message in the view.
     *
     * @param user
     *            the user to be updated
     * @throws IllegalStateException
     *             if the current user is not an admin
     */
    public void updateUser(User user) {
        accessControl.assertAdmin();
        try {
            var updatedUser = getService().updateUser(user);
            var id = updatedUser.getId();
            assert id != null : "User ID should not be null";
            getEventBus().post(new UserUpdatedEvent(id));
            view.showUserUpdated();
            requestUpdateUsers();
            logger.info("User {}/'{}' updated.", user.getId(), user.getName());
        } catch (IllegalArgumentException e) {
            view.showDuplicateError();
        } catch (OptimisticLockException e) {
            requestUpdateUsers();
            view.showSaveConflict();
        }
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

    private UserService getService() {
        return VaadinCreateUI.get().getUserService();
    }

    public record UserUpdatedEvent(Integer userId) {
    }

    private static Logger logger = LoggerFactory
            .getLogger(UserManagementPresenter.class);
}
