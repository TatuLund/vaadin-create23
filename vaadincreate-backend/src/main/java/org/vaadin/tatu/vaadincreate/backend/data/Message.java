package org.vaadin.tatu.vaadincreate.backend.data;

import java.time.LocalDateTime;

import javax.persistence.Entity;

@SuppressWarnings({ "serial", "java:S2160" })
@Entity
public class Message extends AbstractEntity {

    private String message;
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
