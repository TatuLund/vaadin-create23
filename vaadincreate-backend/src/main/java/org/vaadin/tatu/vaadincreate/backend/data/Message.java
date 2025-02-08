package org.vaadin.tatu.vaadincreate.backend.data;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
public class Message extends AbstractEntity {

    @SuppressWarnings("java:S1700")

    @Column(name = "message")
    @NotNull
    private String message;

    @Column(name = "date_stamp")
    @NotNull
    private LocalDateTime dateStamp;

    public Message() {
    }

    public Message(String message, LocalDateTime dateStamp) {
        this.message = message;
        this.dateStamp = dateStamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDateStamp() {
        return dateStamp;
    }

    public void setDateStamp(LocalDateTime dateStamp) {
        this.dateStamp = dateStamp;
    }
}
