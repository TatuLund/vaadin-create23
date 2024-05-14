package org.vaadin.tatu.vaadincreate.backend;

import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.mock.MockAppDataService;

public interface AppDataService {

    public abstract Message updateMessage(String message);

    public abstract Message getMessage();

    public static AppDataService get() {
        return MockAppDataService.getInstance();
    }

}
