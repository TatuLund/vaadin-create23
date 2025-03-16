package org.vaadin.tatu.vaadincreate.backend.events;

import java.time.LocalDateTime;

public record MessageEvent(String message, LocalDateTime timeStamp) implements AbstractEvent {
}


