package org.vaadin.tatu.vaadincreate.about;

import java.time.LocalDateTime;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.VaadinCreateView;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.AllPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.components.CharacterCountExtension;
import org.vaadin.tatu.vaadincreate.components.ConfirmDialog;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.ConfirmDialog.Type;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.observability.Telemetry;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.ShortcutListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.Registration;
import com.vaadin.shared.Version;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@NullMarked
@AllPermitted
@SuppressWarnings({ "serial", "java:S2160" })
public class AboutView extends VerticalLayout implements VaadinCreateView {

    private static final Logger logger = LoggerFactory
            .getLogger(AboutView.class);

    public static final String VIEW_NAME = "about";

    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    private AboutPresenter presenter = new AboutPresenter(this);

    @Nullable
    private Button editButton;
    private Label adminsNote;
    private TextArea adminsNoteField;
    @Nullable
    private Button shutDownButton;

    @Nullable
    private UI ui;

    /**
     * Constructor.
     */
    public AboutView() {
        var aboutContent = createAboutContent();

        adminsNoteField = new AdminsNoteField();
        adminsNoteField.setVisible(false);

        var adminsContent = new HorizontalLayout();
        adminsContent.addStyleName(VaadinCreateTheme.ABOUTVIEW_ADMINSCONTENT);
        adminsContent.setWidth("500px");
        createAdminsNote();

        adminsContent.addComponents(adminsNote, adminsNoteField);
        if (accessControl.isUserInRole(Role.ADMIN)) {
            createEditButton();
            editButton.addClickListener(click -> {
                accessControl.assertAdmin();
                adminsNoteField.setVisible(true);
                Utils.startPolling();
                adminsNote.setVisible(false);
                editButton.setVisible(false);
                adminsNoteField.setValue(adminsNote.getValue());
                adminsNoteField.focus();
            });
            adminsContent.addComponent(editButton);
            adminsContent.setComponentAlignment(editButton,
                    Alignment.TOP_RIGHT);
        }

        setSizeFull();
        setMargin(false);
        setStyleName(VaadinCreateTheme.ABOUT_VIEW);
        addComponents(aboutContent, adminsContent);
        setComponentAlignment(aboutContent, Alignment.MIDDLE_CENTER);
        setComponentAlignment(adminsContent, Alignment.MIDDLE_CENTER);
        if (accessControl.isUserInRole(Role.ADMIN)) {
            createShutdownButton();
            addComponent(shutDownButton);
            setComponentAlignment(shutDownButton, Alignment.MIDDLE_CENTER);
        }
    }

    private void createShutdownButton() {
        shutDownButton = new Button(getTranslation(I18n.About.SHUTDOWN));
        shutDownButton.setId("shutdown-button");
        shutDownButton.setDisableOnClick(true);
        shutDownButton.setIcon(VaadinIcons.POWER_OFF);
        shutDownButton.addStyleNames(ValoTheme.BUTTON_BORDERLESS,
                ValoTheme.BUTTON_SMALL);
        shutDownButton.setDescription(
                getTranslation(I18n.About.SHUTDOWN_DESCRIPTION));
        shutDownButton.addClickListener(click -> handleGlobalLogout());
    }

    private void handleGlobalLogout() {
        accessControl.assertAdmin();
        var confirmDialog = new ConfirmDialog(
                getTranslation(I18n.About.SHUTDOWN),
                getTranslation(I18n.About.CONFIRM_SHUTDOWN), Type.ALERT);
        confirmDialog.setConfirmText(getTranslation(I18n.CONFIRM));
        confirmDialog.setCancelText(getTranslation(I18n.CANCEL));
        confirmDialog.addConfirmedListener(
                confirmed -> presenter.scheduleShutdown());
        confirmDialog.addCancelledListener(
                cancelled -> shutDownButton.setEnabled(true));
        confirmDialog.open();
    }

    private void createEditButton() {
        editButton = new Button();
        editButton.setId("admin-edit");
        editButton.setIcon(VaadinIcons.EDIT);
        editButton.addStyleNames(ValoTheme.BUTTON_BORDERLESS,
                ValoTheme.BUTTON_SMALL);
        editButton.setDescription(getTranslation(I18n.About.EDIT_NOTE));
        AttributeExtension.of(editButton)
                .setAttribute(AriaAttributes.DESCRIBEDBY, "admins-note");
    }

    private void createAdminsNote() {
        adminsNote = new Label();
        adminsNote.setContentMode(ContentMode.HTML);
        adminsNote.addStyleName(VaadinCreateTheme.WHITESPACE_PRE);
        adminsNote.setId("admins-note");
    }

    private CustomLayout createAboutContent() {
        var aboutContent = new CustomLayout("aboutview");
        aboutContent.setStyleName(VaadinCreateTheme.ABOUTVIEW_ABOUTCONTENT);

        var aboutLabel = new Label(
                VaadinIcons.INFO_CIRCLE.getHtml() + getTranslation(
                        I18n.About.VAADIN, Version.getFullVersion()),
                ContentMode.HTML);
        aboutLabel.addStyleName(VaadinCreateTheme.ABOUTVIEW_ABOUTLABEL);
        aboutLabel.setId("info-label");
        // you can add Vaadin components in predefined slots in the custom
        // layout
        aboutContent.addComponent(aboutLabel, "info");
        return aboutContent;
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(VIEW_NAME);

        var message = presenter.fetchMessage();
        if (message != null) {
            var timeStamp = message.getDateStamp();
            adminsNote.setCaption(Utils.formatDate(timeStamp, getLocale()));
            adminsNote.setValue(message.getMessage());
        } else {
            adminsNote.setValue(getTranslation(I18n.About.NO_MESSAGE));
        }
    }

    /**
     * Updates the admin note asynchronously using accessing the UI.
     * 
     * @param message
     *            the new message
     * @param timeStamp
     *            the timestamp of the message
     */
    public void updateAsync(String message, LocalDateTime timeStamp) {
        Utils.access(ui, () -> {
            if (adminsNoteField.isVisible()) {
                adminsNoteField.setVisible(false);
                Utils.stopPolling();
                adminsNote.setVisible(true);
                editButton.setVisible(true);
            }
            adminsNote.setCaption(Utils.formatDate(timeStamp, getLocale()));
            adminsNote.setValue(message);
        });
    }

    /**
     * Enables the shutdown button asynchronously using accessing the UI.
     */
    public void enableShutdownAsync() {
        Utils.access(ui, () -> {
            if (shutDownButton != null) {
                shutDownButton.setEnabled(false);
            }
        });
    }

    @Override
    public void attach() {
        super.attach();
        ui = getUI();
    }

    @Override
    public void detach() {
        super.detach();
        Utils.stopPolling();
        presenter.unregister();
    }

    class AdminsNoteField extends TextArea implements HasAttributes<TextArea> {

        private static final int MAX_LENGTH = 250;
        @Nullable
        private Registration saveRegistration;

        @SuppressWarnings("java:S3878")
        AdminsNoteField() {
            setId("admins-text-area");
            setMaxLength(MAX_LENGTH);
            setWidth("450px");
            setIcon(VaadinIcons.FILE_TEXT_O);
            setCaption("HTML");
            setPlaceholder(getTranslation(I18n.About.ADMIN_NOTE_PLACEHOLDER,
                    MAX_LENGTH));
            saveRegistration = addShortcutListener(new ShortcutListener("Save",
                    KeyCode.S, new int[] { ModifierKey.CTRL }) {
                @Override
                public void handleAction(Object sender, Object target) {
                    setValue(getValue().trim());
                    closeEditor();
                }
            });
            setAttribute(AriaAttributes.KEYSHORTCUTS, "Control+S");
            CharacterCountExtension.extend(this);
            addValueChangeListener(this::handleValueChange);
            setValueChangeMode(ValueChangeMode.BLUR);
            addBlurListener(blurEvent -> closeEditor());
        }

        private void closeEditor() {
            adminsNote.setVisible(true);
            adminsNoteField.setVisible(false);
            editButton.setVisible(true);
        }

        private void handleValueChange(ValueChangeEvent<String> valueChange) {
            if (valueChange.isUserOriginated()) {
                var unsanitized = valueChange.getValue();
                // Sanitize user input with Jsoup to avoid JavaScript injection
                // vulnerabilities
                var text = Utils.sanitize(unsanitized);
                var mes = presenter.updateMessage(text);
                adminsNote.setCaption(
                        Utils.formatDate(mes.getDateStamp(), getLocale()));
                adminsNote.setValue(mes.getMessage());
                logger.info("Admin message updated");
                Telemetry.saveItem(mes);
            }
        }

        @Override
        public void detach() {
            super.detach();
            if (saveRegistration != null) {
                saveRegistration.remove();
                saveRegistration = null;
            }
        }
    }

}
