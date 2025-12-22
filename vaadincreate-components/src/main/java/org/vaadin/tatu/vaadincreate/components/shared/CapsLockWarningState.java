package org.vaadin.tatu.vaadincreate.components.shared;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.shared.communication.SharedState;

@NullMarked
@SuppressWarnings({ "serial", "java:S1104" })
public class CapsLockWarningState extends SharedState {

    @Nullable
    public String message = "Caps Lock is enabled!";
}
