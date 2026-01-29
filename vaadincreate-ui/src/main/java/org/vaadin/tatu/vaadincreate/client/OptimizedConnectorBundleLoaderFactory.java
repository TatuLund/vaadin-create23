package org.vaadin.tatu.vaadincreate.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.vaadin.server.widgetsetutils.ConnectorBundleLoaderFactory;
import com.vaadin.shared.ui.Connect.LoadStyle;

public final class OptimizedConnectorBundleLoaderFactory
        extends ConnectorBundleLoaderFactory {

    private static final Set<String> EAGER_CONNECTORS = Collections
            .unmodifiableSet(createEagerConnectors());

    // These connectors are loaded eagerly to improve initial rendering time
    // of the LoginView
    private static Set<String> createEagerConnectors() {
        Set<String> connectors = new HashSet<>();
        connectors.add(com.vaadin.client.ui.ui.UIConnector.class.getName());
        connectors
                .add(com.vaadin.client.ui.image.ImageConnector.class.getName());
        connectors.add(com.vaadin.client.ui.composite.CompositeConnector.class
                .getName());
        connectors
                .add(com.vaadin.client.ui.label.LabelConnector.class.getName());
        connectors.add(
                com.vaadin.client.ui.passwordfield.PasswordFieldConnector.class
                        .getName());
        connectors.add(
                com.vaadin.client.ui.orderedlayout.VerticalLayoutConnector.class
                        .getName());
        connectors.add(com.vaadin.client.ui.formlayout.FormLayoutConnector.class
                .getName());
        connectors.add(com.vaadin.client.ui.csslayout.CssLayoutConnector.class
                .getName());
        connectors.add(
                com.vaadin.client.ui.button.ButtonConnector.class.getName());
        connectors.add(
                com.vaadin.client.connectors.data.DataCommunicatorConnector.class
                        .getName());
        connectors.add(
                com.vaadin.client.extensions.javascriptmanager.JavaScriptManagerConnector.class
                        .getName());
        connectors.add(
                org.vaadin.tatu.vaadincreate.components.client.CapsLockWarningConnector.class
                        .getName());
        connectors.add(com.vaadin.client.ui.textfield.TextFieldConnector.class
                .getName());
        connectors.add(com.vaadin.client.JavaScriptExtension.class.getName());
        connectors.add(com.vaadin.client.ui.combobox.ComboBoxConnector.class
                .getName());
        return connectors;
    }

    @Override
    protected LoadStyle getLoadStyle(JClassType connectorType) {
        if (EAGER_CONNECTORS.contains(connectorType.getQualifiedBinaryName())) {
            return LoadStyle.EAGER;
        }
        // Loads all other connectors immediately after the initial view
        // has
        // been rendered
        return LoadStyle.DEFERRED;
    }
}
