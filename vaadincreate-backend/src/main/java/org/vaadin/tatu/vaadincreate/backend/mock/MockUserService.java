package org.vaadin.tatu.vaadincreate.backend.mock;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.User;

public class MockUserService implements UserService {

    private static MockUserService INSTANCE;

    private List<User> users;

    public synchronized static UserService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MockUserService();
        }
        return INSTANCE;
    }

    public MockUserService() {
        users = MockDataGenerator.createUsers();
        logger.info("Generated mock user data");
    }

    @Override
    public synchronized Optional<User> findByName(String name) {
        return users.stream().filter(user -> user.getName().equals(name))
                .findFirst();
    }

    @Override
    public synchronized User updateUser(User user) {
        int index = users.indexOf(user);
        if (index > 0) {
            users.remove(index);
            users.add(user);
        } else {
            users.add(user);
        }
        return user;
    }

    @Override
    public synchronized List<User> getAllUsers() {
        return users;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
