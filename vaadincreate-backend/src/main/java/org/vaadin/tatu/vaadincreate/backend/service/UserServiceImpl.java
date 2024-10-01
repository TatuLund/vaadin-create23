package org.vaadin.tatu.vaadincreate.backend.service;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.dao.UserDao;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.mock.MockDataGenerator;

@SuppressWarnings("java:S6548")
public class UserServiceImpl implements UserService {

    private UserDao userDao = new UserDao();
    private Random random = new Random();

    private static UserServiceImpl instance;

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserServiceImpl();
        }
        return instance;
    }

    private UserServiceImpl() {
        var users = MockDataGenerator.createUsers();
        users.forEach(user -> userDao.updateUser(user));
        logger.info("Generated mock user data");
    }

    @Override
    public synchronized User updateUser(User user) {
        Objects.requireNonNull(user, "User must not be null");
        randomWait(2);
        var existingUser = userDao.findByName(user.getName());
        if (existingUser != null
                && !existingUser.getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "User with the same name already exists");
        }
        return userDao.updateUser(user);
    }

    @Override
    public synchronized Optional<User> findByName(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        randomWait(1);
        return Optional.ofNullable(userDao.findByName(name));
    }

    @Override
    public synchronized User getUserById(Integer userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        randomWait(1);
        return userDao.getUserById(userId);
    }

    @Override
    public synchronized void removeUser(Integer userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        randomWait(1);
        userDao.removeUser(userId);
    }

    @Override
    public synchronized java.util.List<User> getAllUsers() {
        randomWait(3);
        return userDao.getAllUsers();
    }

    @SuppressWarnings("java:S2142")
    private void randomWait(int count) {
        int wait = 20 + random.nextInt(30);
        try {
            Thread.sleep(wait * (long) count);
        } catch (InterruptedException e) {
            // NOP
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

}
