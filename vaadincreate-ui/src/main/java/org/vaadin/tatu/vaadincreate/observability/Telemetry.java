package org.vaadin.tatu.vaadincreate.observability;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.backend.data.AbstractEntity;

import com.vaadin.ui.ComponentContainer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Utility class for logging telemetry events using OpenTelemetry.
 */
@NullMarked
public final class Telemetry {
    private static final Tracer tracer = GlobalOpenTelemetry.get()
            .getTracer("vaadincreate");

    private Telemetry() {
    }

    private static Span start(String name) {
        return tracer.spanBuilder(name).setParent(Context.current())
                .startSpan();
    }

    /**
     * Logs an item opened event to telemetry.
     * 
     * @param item
     *            the opened item
     */
    public static void openedItem(AbstractEntity item) {
        item(item, "opened");
    }

    /**
     * Logs an item deleted event to telemetry.
     * 
     * @param item
     *            the deleted item
     */
    public static void deleteItem(AbstractEntity item) {
        item(item, "deleted");
    }

    /**
     * Logs an item saved event to telemetry.
     * 
     * @param item
     *            the saved item
     */
    public static void saveItem(AbstractEntity item) {
        item(item, "saved");
    }

    private static void item(AbstractEntity item, String action) {
        Span span = Span.current();
        boolean started = false;
        if (!span.getSpanContext().isValid()) {
            span = start("item." + action);
            started = true;
        }
        try {
            span.setAttribute("item.type", item.getClass().getSimpleName());
            span.setAttribute("item.action", action);
            int id = item.getId() != null ? item.getId() : -1;
            span.setAttribute("item.id", String.valueOf(id));
            span.addEvent(action.toUpperCase(),
                    Attributes.of(AttributeKey.stringKey("item.id"),
                            String.valueOf(id),
                            AttributeKey.stringKey("item.type"),
                            item.getClass().getSimpleName()));
            span.setStatus(StatusCode.OK);
        } finally {
            if (started) {
                span.end();
            }
        }
    }

    /**
     * Logs a view entered event to telemetry.
     *
     * @param oldView
     *            the previous view, or null if none
     * @param newView
     *            the new view, or null if unknown
     */
    public static void entered(@Nullable ComponentContainer oldView,
            @Nullable ComponentContainer newView) {
        Span span = Span.current();
        boolean started = false;
        if (!span.getSpanContext().isValid()) {
            span = start("view.open");
            started = true;
        }
        try {
            span.setAttribute("view.action", "enter");
            span.setAttribute("view.from",
                    oldView != null ? oldView.getClass().getSimpleName()
                            : "none");
            span.setAttribute("view.to",
                    newView != null ? newView.getClass().getSimpleName()
                            : "unknown");
            span.addEvent("ENTERED",
                    Attributes.of(AttributeKey.stringKey("view.from"),
                            oldView != null ? oldView.getClass().getSimpleName()
                                    : "none",
                            AttributeKey.stringKey("view.to"),
                            newView != null ? newView.getClass().getSimpleName()
                                    : "unknown"));
            span.setStatus(StatusCode.OK);
        } finally {
            if (started) {
                span.end();
            }
        }
    }

    /**
     * Records an exception to telemetry.
     * 
     * @param exception
     */
    public static void exception(Throwable exception) {
        Span span = Span.current();
        boolean started = false;
        if (!span.getSpanContext().isValid()) {
            span = start("exception");
            started = true;
        }
        try {
            span.recordException(exception);
            span.setAttribute("exception.type",
                    exception.getClass().getSimpleName());
            span.setAttribute("error", true);
            span.addEvent("EXCEPTION");
            span.setStatus(StatusCode.ERROR, exception.getMessage());
        } finally {
            if (started) {
                span.end();
            }
        }
    }

}
