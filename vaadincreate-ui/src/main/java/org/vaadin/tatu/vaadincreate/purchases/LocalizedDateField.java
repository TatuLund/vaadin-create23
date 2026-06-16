package org.vaadin.tatu.vaadincreate.purchases;

import java.time.LocalDate;

import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.shared.ui.datefield.AbstractDateFieldState.AccessibleElement;
import com.vaadin.ui.DateField;

@SuppressWarnings("java:S110")
public class LocalizedDateField extends DateField implements HasI18N {

    /**
     * Creates a DateField with localized captions, assistive texts, and error
     * messages for the PurchasesHistoryView. The field is configured to only
     * allow selection of dates up to the current date, and it includes
     * assistive labels for navigating the date picker, as well as custom
     * messages for parsing errors and out-of-range selections.
     *
     * @param caption
     *            the caption for the DateField, which should be localized
     *            before being passed.
     */
    LocalizedDateField(String caption) {
        super(caption);
        setRequiredIndicatorVisible(true);
        setRangeEnd(LocalDate.now());
        setAssistiveText(
                getTranslation(I18n.Purchases.DATE_FIELD_ASSISTIVE_TEXT));
        setAssistiveLabel(AccessibleElement.NEXT_MONTH,
                getTranslation(I18n.Purchases.DATE_FIELD_NEXT_MONTH));
        setAssistiveLabel(AccessibleElement.PREVIOUS_MONTH,
                getTranslation(I18n.Purchases.DATE_FIELD_PREVIOUS_MONTH));
        setAssistiveLabel(AccessibleElement.NEXT_YEAR,
                getTranslation(I18n.Purchases.DATE_FIELD_NEXT_YEAR));
        setAssistiveLabel(AccessibleElement.PREVIOUS_YEAR,
                getTranslation(I18n.Purchases.DATE_FIELD_PREVIOUS_YEAR));
        setParseErrorMessage(
                getTranslation(I18n.Purchases.DATE_FIELD_PARSE_ERROR));
        setDateOutOfRangeMessage(
                getTranslation(I18n.Purchases.DATE_FIELD_OUT_OF_RANGE));
    }
}
