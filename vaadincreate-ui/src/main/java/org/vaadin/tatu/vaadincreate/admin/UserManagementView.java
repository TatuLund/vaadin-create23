package org.vaadin.tatu.vaadincreate.admin;

import java.util.List;

import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.ValidationException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings({ "serial", "java:S2160" })
public class UserManagementView extends VerticalLayout implements TabView {

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
        var attributes = AttributeExtension.of(this);
        attributes.setAttribute("role", "region");
        attributes.setAttribute("aria-labelledby", "view-name");

        var title = new Label(getTranslation(I18n.User.EDIT_USERS));
        title.addStyleName(ValoTheme.LABEL_H4);
        title.setId("view-name");

        form = new UserForm();
        form.setEnabled(false);
        form.addFormChangedListener(event -> {
            save.setEnabled(event.isValid());
            cancel.setEnabled(true);
        });

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
        }
    }

    public void showSaveConflict() {
        Notification.show(getTranslation(I18n.SAVE_CONFLICT),
                Notification.Type.WARNING_MESSAGE);
        form.clear();
        disableButtons();
        userSelect.setValue(null);
    }

    @Override
    public void enter() {
        openingView(VIEW_NAME);
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

}
