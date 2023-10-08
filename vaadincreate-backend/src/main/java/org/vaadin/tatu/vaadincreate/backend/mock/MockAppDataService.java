package org.vaadin.tatu.vaadincreate.backend.mock;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Message;

public class MockAppDataService implements AppDataService {
    private static MockAppDataService INSTANCE;

    private Message message;

    public synchronized static AppDataService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MockAppDataService();
        }
        return INSTANCE;
    }

    public MockAppDataService() {
        message = new Message(MockDataGenerator.createMessage(),
                LocalDateTime.now());
        logger.info("Generated mock app data");
    }

    @Override
    public synchronized Message updateMessage(String message) {
        this.message = new Message(message, LocalDateTime.now());
        return this.message;
    }

    @Override
    public synchronized Message getMessage() {
        return message;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
