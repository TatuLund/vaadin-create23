package org.vaadin.tatu.vaadincreate.admin;

import java.lang.reflect.Method;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.data.ValidationException;
import com.vaadin.event.ConnectorEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.ReflectTools;

/**
 * The form for editing users.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class UserForm extends Composite implements HasI18N {
    private FormLayout form = new FormLayout();
    private BeanValidationBinder<User> binder = new BeanValidationBinder<>(
            User.class);
    private PasswordField password2;
    private TextField username;
    @Nullable
    private User user;
    private ComboBox<Role> role;

    public UserForm() {
        form.addStyleName(VaadinCreateTheme.ADMINVIEW_USERFORM);
        AttributeExtension.of(form).setAttribute("role", "form");
        username = new TextField(getTranslation(I18n.User.USERNAME));
        username.setId("user-field");
        AttributeExtension.of(username).setAttribute("autocomplete", "off");
        var password = new PasswordField(getTranslation(I18n.PASSWORD));
        password.setId("password-field");
        password2 = new PasswordField(getTranslation(I18n.User.PASSWD_REPEAT));
        password2.setId("password-repeat");
        role = new ComboBox<>(getTranslation(I18n.User.ROLE));
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

        password2.addValueChangeListener(valueChange -> {
            if (valueChange.isUserOriginated()) {
                binder.validate();
            }
        });
        password2.setRequiredIndicatorVisible(true);

        binder.bind(role, "role");
        binder.addValueChangeListener(valueChange -> fireEvent(
                new FormChangedEvent(this, binder.isValid())));

        setCompositionRoot(form);
    }

    /**
     * Populates the form with the user's data.
     *
     * @param user
     *            the user to be populated, not null
     */
    public void populate(User user) {
        this.user = user;
        binder.readBean(user);
        password2.setValue(user.getPasswd());
        setEnabled(true);
        username.focus();
        // Cannot change own role
        role.setEnabled(!Utils.getCurrentUserOrThrow().equals(user));
    }

    /**
     * Clears the form.
     */
    public void clear() {
        this.user = null;
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
    public void commit() throws ValidationException {
        assert user != null : "User must not be null when committing";
        binder.writeBean(user);
    }

    /**
     * Add event listener for FormChangedEvent. Event is fired when user is
     * edited.
     *
     * @param listener
     *            The listener, can be Lambda expression.
     * @return Registration Use Registration#remove() for listener removal.
     */
    public Registration addFormChangedListener(FormChangedListener listener) {
        return addListener(FormChangedEvent.class, listener,
                FormChangedListener.FORM_CHANGED_METHOD);
    }

    /**
     * FormChangedEvent listener interface, can be implemented with Lambda or
     * anonymous inner class.
     */
    public interface FormChangedListener extends ConnectorEventListener {
        Method FORM_CHANGED_METHOD = ReflectTools.findMethod(
                FormChangedListener.class, "formChanged",
                FormChangedEvent.class);

        public void formChanged(FormChangedEvent event);
    }

    /**
     * FormChangedEvent is fired when user is edited.
     */
    public static class FormChangedEvent extends Component.Event {

        boolean valid;

        public FormChangedEvent(Component source, boolean valid) {
            super(source);
            this.valid = valid;
        }

        /**
         * Returns whether the form is valid.
         *
         * @return true if the form is valid, false otherwise
         */
        public boolean isValid() {
            return valid;
        }
    }
}
