package org.vaadin.tatu.vaadincreate.crud.form;

import org.vaadin.tatu.vaadincreate.backend.data.Availability;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.ComboBox;

/**
 * A custom Vaadin ComboBox component for selecting availability options. This
 * component is configured with specific styles and behaviors for displaying
 * availability statuses.
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * AvailabilitySelector availabilitySelector = new AvailabilitySelector(
 *         "Availability");
 * </pre>
 */
@SuppressWarnings("serial")
public class AvailabilitySelector extends ComboBox<Availability> {

    public AvailabilitySelector(String caption) {
        setId("availability");
        setCaption(caption);
        setWidthFull();

        setItems(Availability.values());
        setEmptySelectionAllowed(false);
        setTextInputAllowed(false);
        setItemIconGenerator(item -> VaadinIcons.CIRCLE);
        setStyleGenerator(this::getAvailabilityStyle);
        addValueChangeListener(e -> {
            if (e.getOldValue() != null) {
                removeStyleName(getAvailabilityStyle(e.getOldValue()));
            }
            addStyleName(getAvailabilityStyle(e.getValue()));
        });
    }

    private String getAvailabilityStyle(Availability item) {
        return String.format("bookform-availability-%s",
                item.name().toLowerCase());
    }
}