package org.vaadin.tatu.vaadincreate.backend.data;

import java.time.LocalDateTime;

public class Message {

    private String message;
    private LocalDateTime dateStamp;

    public Message(String message, LocalDateTime dateStamp) {
        super();
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
