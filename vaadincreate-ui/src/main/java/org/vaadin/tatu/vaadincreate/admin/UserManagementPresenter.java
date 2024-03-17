package org.vaadin.tatu.vaadincreate.admin;

import java.io.Serializable;

@SuppressWarnings("serial")
public class UserManagementPresenter implements Serializable {

    private UserManagementView view;

    public UserManagementPresenter(UserManagementView view) {
        this.view = view;
    }
}
