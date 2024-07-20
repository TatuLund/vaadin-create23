package org.vaadin.tatu.vaadincreate.admin;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.User;

@SuppressWarnings("serial")
public class UserManagementPresenter implements Serializable {

    private UserManagementView view;
    private UserService service = VaadinCreateUI.get().getUserService();
    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    public UserManagementPresenter(UserManagementView view) {
        this.view = view;
    }

    public void requestUpdateUsers() {
        logger.info("Fetching users");
        view.setUsers(UserService.get().getAllUsers());
    }

    public void removeUser(int id) {
        accessControl.assertAdmin();
        service.removeUser(id);
        logger.info("User '{}' removed.", id);
        view.showUserRemoved();
        requestUpdateUsers();
    }

    public void updateUser(User user) {
        accessControl.assertAdmin();
        try {
            service.updateUser(user);
            view.showUserUpdated();
            requestUpdateUsers();
            logger.info("User {}/'{}' updated.", user.getId(), user.getName());
        } catch (IllegalArgumentException e) {
            view.showDuplicateError();
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
