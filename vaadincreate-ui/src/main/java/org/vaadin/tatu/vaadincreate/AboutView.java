package org.vaadin.tatu.vaadincreate;

import java.time.LocalDateTime;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.AllPermitted;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.data.HasValue.ValueChangeEvent;
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
public class AboutView extends VerticalLayout
        implements VaadinCreateView, EventBusListener {

    public static final String VIEW_NAME = "about";

    private AccessControl accessControl = VaadinCreateUI.get()
            .getAccessControl();

    @Nullable
    private Button editButton;
    private Label adminsNote;
    private TextArea adminsNoteField;

    @Nullable
    private UI ui;

    @Nullable
    private Registration saveRegistration;

    public AboutView() {
        var aboutContent = createAboutContent();

        adminsNoteField = createTextArea();
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
                adminsNote.setVisible(false);
                editButton.setVisible(false);
                adminsNoteField.setValue(adminsNote.getValue());
            });
            adminsContent.addComponent(editButton);
            adminsContent.setComponentAlignment(editButton,
                    Alignment.TOP_RIGHT);
        }

        adminsNoteField.addValueChangeListener(this::handleValueChange);
        adminsNoteField.setValueChangeMode(ValueChangeMode.BLUR);
        adminsNoteField.addBlurListener(blurEvent -> closeEditor());
        setSizeFull();
        setMargin(false);
        setStyleName(VaadinCreateTheme.ABOUT_VIEW);
        addComponents(aboutContent, adminsContent);
        setComponentAlignment(aboutContent, Alignment.MIDDLE_CENTER);
        setComponentAlignment(adminsContent, Alignment.MIDDLE_CENTER);
        getEventBus().registerEventBusListener(this);
    }

    private void closeEditor() {
        adminsNote.setVisible(true);
        adminsNoteField.setVisible(false);
        editButton.setVisible(true);
        if (saveRegistration != null) {
            saveRegistration.remove();
        }
    }

    private void handleValueChange(ValueChangeEvent<String> valueChange) {
        if (valueChange.isUserOriginated()) {
            var unsanitized = valueChange.getValue();
            // Sanitize user input with Jsoup to avoid JavaScript injection
            // vulnerabilities
            var text = Utils.sanitize(unsanitized);
            Message mes = getService().updateMessage(text);
            adminsNote.setCaption(
                    Utils.formatDate(mes.getDateStamp(), getLocale()));
            adminsNote.setValue(mes.getMessage());
            getEventBus().post(
                    new MessageEvent(mes.getMessage(), mes.getDateStamp()));
            logger.info("Admin message updated");
        }
    }

    private void createEditButton() {
        editButton = new Button();
        editButton.setId("admin-edit");
        editButton.setIcon(VaadinIcons.EDIT);
        editButton.addStyleNames(ValoTheme.BUTTON_BORDERLESS,
                ValoTheme.BUTTON_SMALL);
        editButton.setDescription(getTranslation(I18n.About.EDIT_NOTE));
    }

    private void createAdminsNote() {
        adminsNote = new Label();
        adminsNote.setContentMode(ContentMode.HTML);
        adminsNote.addStyleName(VaadinCreateTheme.WHITESPACE_PRE);
        adminsNote.setId("admins-note");
    }

    // Create text area for editing admins note
    @SuppressWarnings("java:S3878")
    private TextArea createTextArea() {
        var textArea = new TextArea();
        textArea.setId("admins-text-area");
        textArea.setMaxLength(250);
        textArea.setWidth("450px");
        textArea.setIcon(VaadinIcons.FILE_TEXT_O);
        textArea.setCaption("HTML");
        textArea.setPlaceholder("max 250 chars");
        textArea.addFocusListener(focused -> saveRegistration = textArea
                .addShortcutListener(new ShortcutListener("Save", KeyCode.S,
                        new int[] { ModifierKey.CTRL }) {
                    @Override
                    public void handleAction(Object sender, Object target) {
                        textArea.setValue(textArea.getValue().trim());
                        closeEditor();
                    }
                }));
        CharacterCountExtension.extend(textArea);
        return textArea;
    }

    private CustomLayout createAboutContent() {
        var aboutContent = new CustomLayout("aboutview");
        aboutContent.setStyleName(VaadinCreateTheme.ABOUTVIEW_ABOUTCONTENT);

        var aboutLabel = new Label(
                VaadinIcons.INFO_CIRCLE.getHtml() + getTranslation(
                        I18n.About.VAADIN, Version.getFullVersion()),
                ContentMode.HTML);
        aboutLabel.addStyleName(VaadinCreateTheme.ABOUTVIEW_ABOUTLABEL);
        // you can add Vaadin components in predefined slots in the custom
        // layout
        aboutContent.addComponent(aboutLabel, "info");
        return aboutContent;
    }

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(VIEW_NAME);

        Message message = getService().getMessage();
        if (message != null) {
            adminsNote.setCaption(
                    Utils.formatDate(message.getDateStamp(), getLocale()));
            adminsNote.setValue(message.getMessage());
        } else {
            adminsNote.setValue(getTranslation(I18n.About.NO_MESSAGE));
        }
    }

    @Override
    public void eventFired(Object event) {
        if (event instanceof MessageEvent message) {
            Utils.access(ui, () -> {
                if (adminsNoteField.isVisible()) {
                    adminsNoteField.setVisible(false);
                    adminsNote.setVisible(true);
                    editButton.setVisible(true);
                }
                adminsNote.setCaption(
                        Utils.formatDate(message.timeStamp(), getLocale()));
                adminsNote.setValue(message.message());
            });
        }
    }

    @Override
    public void attach() {
        super.attach();
        ui = getUI();
    }

    @Override
    public void detach() {
        super.detach();
        getEventBus().unregisterEventBusListener(this);
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

    private AppDataService getService() {
        return VaadinCreateUI.get().getAppService();
    }

    public record MessageEvent(String message, LocalDateTime timeStamp) {
    }

    private static Logger logger = LoggerFactory.getLogger(AboutView.class);
}
