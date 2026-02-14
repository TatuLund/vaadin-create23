package org.vaadin.tatu.vaadincreate.admin;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension;
import org.vaadin.tatu.vaadincreate.components.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.components.Shortcuts;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.observability.Telemetry;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.ValidationException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
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

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class UserManagementView extends VerticalLayout
        implements TabView, HasAttributes<UserManagementView> {

    public static final String VIEW_NAME = "users";

    @Nullable
    private User user;
    private ComboBox<User> userSelect = new ComboBox<>();

    private UserManagementPresenter presenter = new UserManagementPresenter(
            this);

    private UserForm form;

    private Button save = new Button(getTranslation(I18n.SAVE));
    private Button delete = new Button(getTranslation(I18n.DELETE));
    private Button cancel = new Button(getTranslation(I18n.CANCEL));

    @Nullable
    private Button newUser;

    public UserManagementView() {
        addStyleName(VaadinCreateTheme.ADMINVIEW_USERVIEW);
        setRole(AriaRoles.REGION);
        setAttribute(AriaAttributes.LABELLEDBY, "view-name");

        var title = new Label(getTranslation(I18n.User.EDIT_USERS));
        title.addStyleName(ValoTheme.LABEL_H4);
        title.setId("view-name");

        form = new UserForm();
        form.setEnabled(false);
        form.addFormChangedListener(changed -> {
            save.setEnabled(changed.isValid());
            cancel.setEnabled(true);
            addStyleName(VaadinCreateTheme.ADMINVIEW_USERFORM_CHANGES);
            Utils.startPolling();
        });

        var header = createHeader();
        var buttons = createButtonsLayout();

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
        userSelect.addValueChangeListener(valueChange -> {
            if (valueChange.isUserOriginated()) {
                userSelected(valueChange.getValue());
            }
        });
        AttributeExtension.of(userSelect).setAttribute(AriaAttributes.LABEL,
                getTranslation(I18n.User.SEARCH));
        newUser = new Button(getTranslation(I18n.User.NEW_USER));
        newUser.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        newUser.setId("new-button");
        newUser.setIcon(VaadinIcons.PLUS_CIRCLE);
        newUser.addClickListener(event -> newUser());
        header.addComponents(newUser, userSelect);
        return header;
    }

    private HorizontalLayout createButtonsLayout() {
        var buttons = new HorizontalLayout();

        // Save button is enabled when the form is valid
        save.addStyleName(ValoTheme.BUTTON_PRIMARY);
        save.setClickShortcut(KeyCode.S, ModifierKey.CTRL);
        AttributeExtension.of(save).setAttribute(AriaAttributes.KEYSHORTCUTS,
                "Control+S");
        save.setId("save-button");
        save.addClickListener(click -> handleSave());

        // Delete button is enabled when a user is selected
        delete.addStyleName(ValoTheme.BUTTON_DANGER);
        delete.setId("delete-button");
        delete.addClickListener(click -> handleDelete());

        // Cancel button is enabled when a user is being edited or a new user is
        // being created
        cancel.setId("cancel-button");
        AttributeExtension.of(cancel).setAttribute(AriaAttributes.KEYSHORTCUTS,
                "Escape");
        cancel.addStyleName(VaadinCreateTheme.BUTTON_CANCEL);
        cancel.addClickListener(click -> handleCancel());

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
        Utils.stopPolling();
        this.user = user;
        form.populate(user);
        delete.setEnabled(true);
        save.setEnabled(false);
        cancel.setEnabled(false);
        removeStyleName(VaadinCreateTheme.ADMINVIEW_USERFORM_CHANGES);
    }

    private void handleCancel() {
        form.clear();
        form.setEnabled(false);
        disableButtons();
        userSelect.setValue(null);
        removeStyleName(VaadinCreateTheme.ADMINVIEW_USERFORM_CHANGES);
    }

    private void handleDelete() {
        // Show a confirmation dialog before deleting the user
        var dialog = new ConfirmDialog(getTranslation(I18n.CONFIRM),
                getTranslation(I18n.WILL_DELETE, user.getName()),
                ConfirmDialog.Type.ALERT);
        dialog.setConfirmText(getTranslation(I18n.DELETE));
        dialog.setCancelText(getTranslation(I18n.CANCEL));
        dialog.open();
        dialog.addConfirmedListener(confirmed -> {
            // If the user confirms the deletion, remove the user
            presenter.removeUser(user.getId());
            form.clear();
            disableButtons();
            userSelect.setValue(null);
            removeStyleName(VaadinCreateTheme.ADMINVIEW_USERFORM_CHANGES);
            Telemetry.deleteItem(user);
        });
    }

    private void disableButtons() {
        delete.setEnabled(false);
        save.setEnabled(false);
        cancel.setEnabled(false);
        if (getUI() != null) {
            Utils.stopPolling();
        }
        removeStyleName(VaadinCreateTheme.ADMINVIEW_USERFORM_CHANGES);
    }

    private void handleSave() {
        try {
            // Commit the form to the user object
            form.commit();
            assert user != null : "User must not be null when saving";
            // Update the user in the backend
            presenter.updateUser(user);
            form.clear();
            disableButtons();
            userSelect.setValue(null);
            Telemetry.saveItem(user);
        } catch (ValidationException e1) {
            // NOP
        }
    }

    @Override
    public void detach() {
        super.detach();
        Utils.stopPolling();
        cancel.removeClickShortcut();
        save.removeClickShortcut();
    }

    public void showSaveConflict() {
        Notification.show(getTranslation(I18n.SAVE_CONFLICT),
                Notification.Type.WARNING_MESSAGE);
        form.clear();
        disableButtons();
        userSelect.setValue(null);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(event);
        presenter.requestUpdateUsers();
        newUser.focus();
        Shortcuts.setEscapeShortcut(".adminview-userview", "#cancel-button");
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
