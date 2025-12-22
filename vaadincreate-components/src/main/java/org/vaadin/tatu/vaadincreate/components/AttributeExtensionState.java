package org.vaadin.tatu.vaadincreate.components;

import com.vaadin.shared.JavaScriptExtensionState;

import java.util.ArrayList;
import java.util.HashMap;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Shared state class for {@link AttributeExtension} communication from server
 * to client.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S1104", "java:S1319" })
public class AttributeExtensionState extends JavaScriptExtensionState {
    @Nullable
    public HashMap<String, String> attributes = new HashMap<>();
    @Nullable
    public ArrayList<String> removals = new ArrayList<>();
}
