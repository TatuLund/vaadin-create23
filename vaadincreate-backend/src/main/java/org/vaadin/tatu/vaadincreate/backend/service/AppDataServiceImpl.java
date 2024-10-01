package org.vaadin.tatu.vaadincreate.backend.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.mock.MockDataGenerator;

@SuppressWarnings("java:S6548")
public class AppDataServiceImpl implements AppDataService {
    private static AppDataServiceImpl instance;

    private Message message;

    public static synchronized AppDataService getInstance() {
        if (instance == null) {
            instance = new AppDataServiceImpl();
        }
        return instance;
    }

    private AppDataServiceImpl() {
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
