package org.vaadin.tatu.vaadincreate.backend.service;

import java.time.LocalDateTime;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.dao.MessageDao;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.mock.MockDataGenerator;

@NullMarked
@SuppressWarnings("java:S6548")
public class AppDataServiceImpl implements AppDataService {
    private static AppDataServiceImpl instance;

    private MessageDao messageDao = new MessageDao();

    public static synchronized AppDataService getInstance() {
        if (instance == null) {
            instance = new AppDataServiceImpl();
        }
        return instance;
    }

    private AppDataServiceImpl() {
        var env = System.getProperty("generate.data");
        if (env == null || env.equals("true")) {
            var message = new Message(MockDataGenerator.createMessage(),
                    LocalDateTime.now());
            messageDao.updateMessage(message);
            logger.info("Generated mock app data");
        }
    }

    @Override
    public synchronized Message updateMessage(String message) {
        var messageEntity = new Message(message, LocalDateTime.now());
        return messageDao.updateMessage(messageEntity);
    }

    @Nullable
    @Override
    public synchronized Message getMessage() {
        return messageDao.getLastMessage();
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

}
