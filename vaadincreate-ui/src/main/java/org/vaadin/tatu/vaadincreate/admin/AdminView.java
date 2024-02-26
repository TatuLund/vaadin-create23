package org.vaadin.tatu.vaadincreate.admin;

import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import com.vaadin.navigator.View;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
@RolesPermitted({ Role.ADMIN })
public class AdminView extends VerticalLayout implements View {

    public static final String VIEW_NAME = "admin";

    private AdminPresenter presenter = new AdminPresenter(this);

    public AdminView() {
        addComponents(new Label("TODO"));
    }
}
