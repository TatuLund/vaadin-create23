package org.vaadin.tatu.vaadincreate.crud;

import org.vaadin.tatu.vaadincreate.backend.data.Availability;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.ComboBox;

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
        setStyleGenerator(
                item -> "bookform-availability-" + item.name().toLowerCase());
        addValueChangeListener(e -> {
            if (e.getOldValue() != null) {
                removeStyleName("bookform-availability-"
                        + e.getOldValue().name().toLowerCase());
            }
            addStyleName("bookform-availability-"
                    + e.getValue().name().toLowerCase());
        });
    }
}
