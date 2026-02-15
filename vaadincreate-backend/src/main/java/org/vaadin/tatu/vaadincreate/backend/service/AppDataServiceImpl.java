package org.vaadin.tatu.vaadincreate.backend.service;

import java.time.LocalDateTime;
import java.util.Objects;

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

    @Nullable
    private static AppDataServiceImpl instance;

    private final MessageDao messageDao;

    @SuppressWarnings("null")
    public static synchronized AppDataService getInstance() {
        if (instance == null) {
            instance = new AppDataServiceImpl();
        }
        return instance;
    }

    private AppDataServiceImpl() {
        this.messageDao = new MessageDao();
        var env = System.getProperty("generate.data");
        if (env == null || env.equals("true")) {
            var message = new Message(MockDataGenerator.createMessage(),
                    getNow());
            messageDao.updateMessage(message);
            logger.info("Generated mock app data");
        }
    }

    @Override
    public Message updateMessage(String message) {
        Objects.requireNonNull(message, "Message cannot be null");
        var messageEntity = new Message(message, getNow());
        return messageDao.updateMessage(messageEntity);
    }

    @SuppressWarnings("null")
    private static LocalDateTime getNow() {
        return LocalDateTime.now();
    }

    @Nullable
    @Override
    public Message getMessage() {
        return messageDao.getLastMessage();
    }

    @SuppressWarnings("null")
    private Logger logger = LoggerFactory.getLogger(this.getClass());

}
