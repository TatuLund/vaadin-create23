package org.vaadin.tatu.vaadincreate.purchases;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Modal sub-window for entering an approval or rejection decision comment.
 * Approve allows an optional comment; reject requires a non-empty reason.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class DecisionWindow extends Window implements HasI18N {

    /** Identifies the decision window. */
    public static final String DECISION_WINDOW_ID = "decision-window";
    /** Identifies the decision comment text area. */
    public static final String DECISION_COMMENT_ID = "decision-comment-field";
    /** Identifies the confirm (approve/reject) action button. */
    public static final String CONFIRM_BUTTON_ID = "decision-confirm-button";
    /** Identifies the cancel button. */
    public static final String CANCEL_BUTTON_ID = "decision-cancel-button";

    /**
     * Functional interface invoked when the user confirms the decision.
     */
    @FunctionalInterface
    public interface DecisionConfirmedListener {
        /**
         * Called when the decision is confirmed.
         *
         * @param comment
         *            the decision comment, may be {@code null} for approvals
         */
        void onConfirmed(@Nullable String comment);
    }

    private final boolean isApprove;
    private final TextArea commentField;
    private final Button confirmButton;
    private boolean confirmed = false;

    /**
     * Creates a new {@code DecisionWindow}.
     *
     * @param isApprove
     *            {@code true} for an approval dialog, {@code false} for
     *            rejection
     * @param listener
     *            callback invoked with the comment when the user confirms
     */
    public DecisionWindow(boolean isApprove,
            DecisionConfirmedListener listener) {
        Objects.requireNonNull(listener, "Listener must not be null");
        this.isApprove = isApprove;

        setModal(true);
        setClosable(true);
        setResizable(false);
        setWidth("400px");
        setId(DECISION_WINDOW_ID);
        setCaption(isApprove ? getTranslation(I18n.Storefront.APPROVE)
                : getTranslation(I18n.Storefront.REJECT));

        commentField = buildCommentField();
        confirmButton = buildConfirmButton(listener);
        var cancelButton = buildCancelButton();

        var buttonBar = new HorizontalLayout(confirmButton, cancelButton);
        buttonBar.setSpacing(true);

        var content = new VerticalLayout(commentField, buttonBar);
        content.setComponentAlignment(buttonBar, Alignment.BOTTOM_RIGHT);
        content.setMargin(true);
        content.setSpacing(true);

        setContent(content);
    }

    private TextArea buildCommentField() {
        var field = new TextArea(
                getTranslation(I18n.Storefront.DECISION_COMMENT));
        field.setId(DECISION_COMMENT_ID);
        field.setWidth("100%");
        field.setRows(3);
        field.setValueChangeMode(ValueChangeMode.EAGER);
        if (!isApprove) {
            // For rejection, disable confirm until a reason is entered.
            field.addValueChangeListener(e -> updateConfirmButtonState(field));
        }
        return field;
    }

    private Button buildConfirmButton(DecisionConfirmedListener listener) {
        var label = isApprove ? getTranslation(I18n.Storefront.APPROVE)
                : getTranslation(I18n.Storefront.REJECT);
        var button = new Button(label, e -> {
            confirmed = true;
            var comment = commentField.getValue().trim();
            close();
            listener.onConfirmed(comment.isEmpty() ? null : comment);
        });
        button.setId(CONFIRM_BUTTON_ID);
        button.addStyleName(isApprove ? ValoTheme.BUTTON_PRIMARY
                : ValoTheme.BUTTON_DANGER);
        button.setDisableOnClick(true);
        // For rejection the button is initially disabled (reason required).
        if (!isApprove) {
            button.setEnabled(false);
        }
        return button;
    }

    private Button buildCancelButton() {
        var button = new Button(getTranslation(I18n.CANCEL),
                e -> close());
        button.setId(CANCEL_BUTTON_ID);
        return button;
    }

    private void updateConfirmButtonState(TextArea field) {
        confirmButton.setEnabled(!field.getValue().trim().isEmpty());
    }

    /**
     * Returns {@code true} if the user confirmed the decision (clicked the
     * confirm button), {@code false} if the window was closed via cancel or the
     * X button.
     *
     * @return whether the decision was confirmed
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
