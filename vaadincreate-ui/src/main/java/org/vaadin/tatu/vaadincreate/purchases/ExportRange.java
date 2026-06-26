package org.vaadin.tatu.vaadincreate.purchases;

import java.io.Serializable;
import java.time.LocalDate;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Date range bean used by the purchase history export Binder.
 */
@NullMarked
public class ExportRange implements Serializable {

    @Nullable
    private LocalDate from;
    @Nullable
    private LocalDate to;

    @Nullable
    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(@Nullable LocalDate from) {
        this.from = from;
    }

    @Nullable
    public LocalDate getTo() {
        return to;
    }

    public void setTo(@Nullable LocalDate to) {
        this.to = to;
    }
}
