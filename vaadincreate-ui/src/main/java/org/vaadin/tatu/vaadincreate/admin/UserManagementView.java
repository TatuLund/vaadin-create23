package org.vaadin.tatu.vaadincreate.admin;

import java.util.List;

import javax.persistence.OptimisticLockException;

import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.ValidationException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class UserManagementView extends VerticalLayout
        implements TabView, HasI18N {

    public static final String VIEW_NAME = "users";

    private static final String NOT_MATCHING = "not-matching";
    private static final String USERNAME = "username";
    private static final String PASSWD = "password";
    private static final String PASSWD_REPEAT = "password-repeat";
    private static final String ROLE = "role";
    private static final String SEARCH = "search";
    private static final String SAVE = "save";
    private static final String DELETE = "delete";
    private static final String CANCEL = "cancel";
    private static final String USER_DELETED = "user-deleted";
    private static final String USER_SAVED = "user-saved";
    private static final String USER_IS_DUPLICATE = "user-is-duplicate";
    private static final String NEW_USER = "new-user";
    private static final String EDIT_USERS = "edit-users";
    private static final String WILL_DELETE = "will-delete";
    private static final String SAVE_CONFLICT = "save-conflict";

    private BeanValidationBinder<User> binder = new BeanValidationBinder<>(
            User.class);
    private User user;
    private ComboBox<User> userSelect = new ComboBox<>();

    private UserManagementPresenter presenter = new UserManagementPresenter(
            this);

    private PasswordField password2;

    public UserManagementView() {
        var h4 = new Label(getTranslation(EDIT_USERS));
        h4.addStyleName(ValoTheme.LABEL_H4);
        var header = new HorizontalLayout();
        var buttons = new HorizontalLayout();
        var form = createUserForm();
        form.setEnabled(false);
        var save = new Button(getTranslation(SAVE));
        save.setEnabled(false);
        var delete = new Button(getTranslation(DELETE));
        delete.addStyleName(ValoTheme.BUTTON_DANGER);
        delete.setId("delete-button");
        delete.setEnabled(false);
        userSelect.setEmptySelectionAllowed(false);
        userSelect.setItemCaptionGenerator(User::getName);
        userSelect.setPlaceholder(getTranslation(SEARCH));
        userSelect.setId("user-select");
        userSelect.addValueChangeListener(event -> {
            if (event.isUserOriginated()) {
                user = event.getValue();
                populateForm(form);
                delete.setEnabled(true);
                save.setEnabled(false);
            }
        });
        var newUser = new Button(getTranslation(NEW_USER));
        newUser.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        newUser.setId("new-button");
        newUser.setIcon(VaadinIcons.PLUS_CIRCLE);
        newUser.addClickListener(event -> {
            user = new User();
            populateForm(form);
            delete.setEnabled(false);
            save.setEnabled(false);
        });
        header.addComponents(newUser, userSelect);
        save.addStyleName(ValoTheme.BUTTON_PRIMARY);
        save.setClickShortcut(KeyCode.ENTER);
        save.setId("save-button");
        save.addClickListener(event -> {
            try {
                binder.writeBean(user);
                presenter.updateUser(user);
                clearForm(form);
                delete.setEnabled(false);
                save.setEnabled(false);
                userSelect.setValue(null);
            } catch (ValidationException e1) {
                // NOP
            } catch (OptimisticLockException e) {
                Notification.show(getTranslation(SAVE_CONFLICT),
                        Notification.Type.WARNING_MESSAGE);
                presenter.requestUpdateUsers();
                clearForm(form);
                delete.setEnabled(false);
                save.setEnabled(false);
                userSelect.setValue(null);
            }
        });
        delete.addClickListener(event -> {
            var dialog = new ConfirmDialog(
                    getTranslation(WILL_DELETE, user.getName()),
                    ConfirmDialog.Type.ALERT);
            dialog.setConfirmText(getTranslation(DELETE));
            dialog.setCancelText(getTranslation(CANCEL));
            dialog.open();
            dialog.addConfirmedListener(e -> {
                presenter.removeUser(user.getId());
                clearForm(form);
                delete.setEnabled(false);
                save.setEnabled(false);
                userSelect.setValue(null);
            });
        });
        binder.addValueChangeListener(event -> {
            if (binder.isValid()) {
                save.setEnabled(true);
            } else {
                save.setEnabled(false);
            }
        });
        buttons.addComponents(delete, save);
        buttons.setComponentAlignment(save, Alignment.MIDDLE_RIGHT);
        buttons.setWidthFull();
        addComponents(h4, header, form, buttons);
        setComponentAlignment(buttons, Alignment.BOTTOM_LEFT);
        setComponentAlignment(form, Alignment.TOP_LEFT);
        setExpandRatio(form, 1);
        setSizeFull();
    }

    private void populateForm(FormLayout form) {
        binder.readBean(user);
        password2.setValue(user.getPasswd());
        form.setEnabled(true);
    }

    private void clearForm(FormLayout form) {
        binder.readBean(null);
        password2.setValue("");
        form.setEnabled(false);
    }

    private FormLayout createUserForm() {
        var form = new FormLayout();
        form.addStyleName(VaadinCreateTheme.ADMINVIEW_USERFORM);
        var username = new TextField(getTranslation(USERNAME));
        username.setId("user-field");
        var userNameExt = new AttributeExtension();
        userNameExt.extend(username);
        userNameExt.setAttribute("autocomplete", "242343243");
        var password = new PasswordField(getTranslation(PASSWD));
        password.setId("password-field");
        password2 = new PasswordField(getTranslation(PASSWD_REPEAT));
        password2.setId("password-repeat");
        var role = new ComboBox<Role>(getTranslation(ROLE));
        role.setItems(Role.values());
        role.setEmptySelectionAllowed(false);
        role.setTextInputAllowed(false);
        role.setId("role-field");
        form.addComponents(username, password, password2, role);
        form.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);

        binder.bind(username, "name");
        binder.forField(password)
                .withValidator(value -> value.equals(password2.getValue()),
                        getTranslation(NOT_MATCHING))
                .bind("passwd");

        password2.addValueChangeListener(event -> {
            if (event.isUserOriginated()) {
                binder.validate();
            }
        });
        password2.setRequiredIndicatorVisible(true);

        binder.bind(role, "role");

        return form;
    }

    @Override
    public void enter() {
        presenter.requestUpdateUsers();
    }

    @Override
    public String getTabName() {
        return VIEW_NAME;
    }

    /**
     * Sets the list of users for the user select component.
     *
     * @param allUsers
     *            the list of users to be set
     */
    public void setUsers(List<User> allUsers) {
        userSelect.setItems(allUsers);
    }

    /**
     * Shows an error notification indicating that the user is a duplicate. The
     * notification message is constructed using the user's name.
     */
    public void showDuplicateError() {
        Notification.show(getTranslation(USER_IS_DUPLICATE, user.getName()),
                Type.ERROR_MESSAGE);
    }

    /**
     * Shows a notification indicating that the user has been updated.
     */
    public void showUserUpdated() {
        Notification.show(getTranslation(USER_SAVED, user.getName()));
    }

    /**
     * Shows a notification indicating that a user has been removed.
     */
    public void showUserRemoved() {
        Notification.show(getTranslation(USER_DELETED, user.getName()));
    }

}
