package org.vaadin.tatu.vaadincreate.backend.data;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
public class Message extends AbstractEntity {

    @SuppressWarnings("java:S1700")

    @Column(name = "message")
    @NotNull
    @Nullable
    private String message;

    @Column(name = "date_stamp")
    @Nullable
    private LocalDateTime dateStamp;

    public Message() {
    }

    /**
     * Constructs a new Message with the specified message and date stamp.
     *
     * @param message
     *            the message content
     * @param dateStamp
     *            the date and time the message was created
     */
    public Message(String message, LocalDateTime dateStamp) {
        this.message = message;
        this.dateStamp = dateStamp;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Nullable
    public LocalDateTime getDateStamp() {
        return dateStamp;
    }

    public void setDateStamp(LocalDateTime dateStamp) {
        this.dateStamp = dateStamp;
    }
}
