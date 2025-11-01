package org.vaadin.tatu.vaadincreate.components;

import com.vaadin.shared.JavaScriptExtensionState;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Shared state class for {@link AttributeExtension} communication from server
 * to client.
 */
@SuppressWarnings({ "serial", "java:S1104", "java:S1319" })
public class AttributeExtensionState extends JavaScriptExtensionState {
    public HashMap<String, String> attributes = new HashMap<>();
    public ArrayList<String> removals = new ArrayList<>();
}
