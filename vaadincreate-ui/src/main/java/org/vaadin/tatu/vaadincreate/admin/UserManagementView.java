package org.vaadin.tatu.vaadincreate.admin;

import java.util.List;

import javax.persistence.OptimisticLockException;

import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.ValidationException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Composite;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings({ "serial", "java:S2160" })
public class UserManagementView extends VerticalLayout
        implements TabView, HasI18N {

    public static final String VIEW_NAME = "users";

    private User user;
    private ComboBox<User> userSelect = new ComboBox<>();

    private UserManagementPresenter presenter = new UserManagementPresenter(
            this);

    private UserForm form;

    private Button save;
    private Button delete;
    private Button cancel;

    public UserManagementView() {
        var title = new Label(getTranslation(I18n.User.EDIT_USERS));
        title.addStyleName(ValoTheme.LABEL_H4);
        form = new UserForm();
        form.setEnabled(false);

        var header = createHeader();
        var buttons = createButtons();

        addComponents(title, header, form, buttons);
        setComponentAlignment(buttons, Alignment.BOTTOM_LEFT);
        setComponentAlignment(form, Alignment.TOP_LEFT);
        setExpandRatio(form, 1);
        setSizeFull();
    }

    private HorizontalLayout createHeader() {
        var header = new HorizontalLayout();
        userSelect.setEmptySelectionAllowed(false);
        userSelect.setItemCaptionGenerator(User::getName);
        userSelect.setPlaceholder(getTranslation(I18n.User.SEARCH));
        userSelect.setId("user-select");
        userSelect.addValueChangeListener(event -> {
            if (event.isUserOriginated()) {
                userSelected(event.getValue());
            }
        });
        var newUser = new Button(getTranslation(I18n.User.NEW_USER));
        newUser.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        newUser.setId("new-button");
        newUser.setIcon(VaadinIcons.PLUS_CIRCLE);
        newUser.addClickListener(event -> newUser());
        header.addComponents(newUser, userSelect);
        return header;
    }

    private HorizontalLayout createButtons() {
        var buttons = new HorizontalLayout();

        // Save button is enabled when the form is valid
        save = new Button(getTranslation(I18n.SAVE));
        save.addStyleName(ValoTheme.BUTTON_PRIMARY);
        save.setClickShortcut(KeyCode.ENTER);
        save.setId("save-button");
        save.addClickListener(event -> handleSave());

        // Delete button is enabled when a user is selected
        delete = new Button(getTranslation(I18n.DELETE));
        delete.addStyleName(ValoTheme.BUTTON_DANGER);
        delete.setId("delete-button");
        delete.addClickListener(event -> handleDelete());

        // Cancel button is enabled when a user is being edited or a new user is
        // being created
        cancel = new Button(getTranslation(I18n.CANCEL));
        cancel.setId("cancel-button");
        cancel.setClickShortcut(KeyCode.ESCAPE);
        cancel.addStyleName(VaadinCreateTheme.BUTTON_CANCEL);
        cancel.addClickListener(e -> handleCancel());

        // Initially all buttons are disabled
        disableButtons();

        buttons.addComponents(delete, cancel, save);
        buttons.setComponentAlignment(delete, Alignment.MIDDLE_LEFT);
        buttons.setComponentAlignment(cancel, Alignment.MIDDLE_LEFT);
        buttons.setComponentAlignment(save, Alignment.MIDDLE_RIGHT);
        buttons.setExpandRatio(save, 1);
        buttons.setWidthFull();
        return buttons;
    }

    private void newUser() {
        user = new User();
        form.populate(user);
        delete.setEnabled(false);
        save.setEnabled(false);
        cancel.setEnabled(true);
    }

    private void userSelected(User user) {
        this.user = user;
        form.populate(user);
        delete.setEnabled(true);
        save.setEnabled(false);
        cancel.setEnabled(false);
    }

    private void handleCancel() {
        form.clear();
        form.setEnabled(false);
        disableButtons();
        userSelect.setValue(null);
    }

    private void handleDelete() {
        // Show a confirmation dialog before deleting the user
        var dialog = new ConfirmDialog(
                getTranslation(I18n.WILL_DELETE, user.getName()),
                ConfirmDialog.Type.ALERT);
        dialog.setConfirmText(getTranslation(I18n.DELETE));
        dialog.setCancelText(getTranslation(I18n.CANCEL));
        dialog.open();
        dialog.addConfirmedListener(e -> {
            // If the user confirms the deletion, remove the user
            presenter.removeUser(user.getId());
            form.clear();
            disableButtons();
            userSelect.setValue(null);
        });
    }

    private void disableButtons() {
        delete.setEnabled(false);
        save.setEnabled(false);
        cancel.setEnabled(false);
    }

    private void handleSave() {
        try {
            // Commit the form to the user object
            form.commit();
            // Update the user in the backend
            presenter.updateUser(user);
            form.clear();
            disableButtons();
            userSelect.setValue(null);
        } catch (ValidationException e1) {
            // NOP
        } catch (OptimisticLockException e) {
            // Show a warning notification if the user was updated by someone
            // else
            Notification.show(getTranslation(I18n.SAVE_CONFLICT),
                    Notification.Type.WARNING_MESSAGE);
            presenter.requestUpdateUsers();
            form.clear();
            disableButtons();
            userSelect.setValue(null);
        }
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
        Notification.show(
                getTranslation(I18n.User.USER_IS_DUPLICATE, user.getName()),
                Type.ERROR_MESSAGE);
    }

    /**
     * Shows a notification indicating that the user has been updated.
     */
    public void showUserUpdated() {
        Notification.show(getTranslation(I18n.User.USER_SAVED, user.getName()));
    }

    /**
     * Shows a notification indicating that a user has been removed.
     */
    public void showUserRemoved() {
        Notification
                .show(getTranslation(I18n.User.USER_DELETED, user.getName()));
    }

    /**
     * The form for editing users.
     */
    class UserForm extends Composite {
        private FormLayout form = new FormLayout();
        private BeanValidationBinder<User> binder = new BeanValidationBinder<>(
                User.class);
        private PasswordField password2;
        private TextField username;

        UserForm() {
            form.addStyleName(VaadinCreateTheme.ADMINVIEW_USERFORM);
            username = new TextField(getTranslation(I18n.User.USERNAME));
            username.setId("user-field");
            var userNameExt = new AttributeExtension();
            userNameExt.extend(username);
            userNameExt.setAttribute("autocomplete", "242343243");
            var password = new PasswordField(getTranslation(I18n.PASSWORD));
            password.setId("password-field");
            password2 = new PasswordField(
                    getTranslation(I18n.User.PASSWD_REPEAT));
            password2.setId("password-repeat");
            var role = new ComboBox<Role>(getTranslation(I18n.User.ROLE));
            role.setItems(Role.values());
            role.setEmptySelectionAllowed(false);
            role.setTextInputAllowed(false);
            role.setId("role-field");
            form.addComponents(username, password, password2, role);
            form.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);

            binder.bind(username, "name");
            binder.forField(password)
                    .withValidator(value -> value.equals(password2.getValue()),
                            getTranslation(I18n.User.NOT_MATCHING))
                    .bind("passwd");

            password2.addValueChangeListener(event -> {
                if (event.isUserOriginated()) {
                    binder.validate();
                }
            });
            password2.setRequiredIndicatorVisible(true);

            binder.bind(role, "role");
            binder.addValueChangeListener(event -> {
                save.setEnabled(binder.isValid());
                cancel.setEnabled(true);
            });

            setCompositionRoot(form);
        }

        /**
         * Populates the form with the user's data.
         *
         * @param user
         *            the user to be populated
         */
        void populate(User user) {
            binder.readBean(user);
            password2.setValue(user.getPasswd());
            setEnabled(true);
            username.focus();
        }

        /**
         * Clears the form.
         */
        void clear() {
            binder.readBean(null);
            password2.setValue("");
            setEnabled(false);
        }

        /**
         * Commits the changes made in the form to the underlying data object.
         *
         * @throws ValidationException
         *             if the data in the form fails validation
         */
        void commit() throws ValidationException {
            binder.writeBean(user);
        }
    }
}
