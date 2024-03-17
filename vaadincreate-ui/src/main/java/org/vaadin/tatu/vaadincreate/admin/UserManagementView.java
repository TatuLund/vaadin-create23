package org.vaadin.tatu.vaadincreate.admin;

import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class UserManagementView extends VerticalLayout
        implements TabView, HasI18N {

    public static final String VIEW_NAME = "users";

    public UserManagementView() {
        addComponent(new Label("TODO"));
    }

    @Override
    public String getTabName() {
        return VIEW_NAME;
    }

}
