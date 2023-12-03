package org.vaadin.tatu.vaadincreate.backend.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.User;

public class MockUserService implements UserService {

    private static MockUserService INSTANCE;

    private List<User> users;
    private int nextUserId = 0;

    private Random random = new Random();

    public synchronized static UserService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MockUserService();
        }
        return INSTANCE;
    }

    public MockUserService() {
        users = MockDataGenerator.createUsers();
        nextUserId = users.get(users.size() - 1).getId() + 1;
        logger.info("Generated mock user data");
    }

    @Override
    public synchronized Optional<User> findByName(String name) {
        randomWait(1);
        var optUser = users.stream().filter(user -> user.getName().equals(name))
                .findFirst();
        if (optUser.isPresent()) {
            var user = optUser.get();
            return Optional.of(new User(user.getId(), user.getName(),
                    user.getPasswd(), user.getRole()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public synchronized User updateUser(User user) {
        randomWait(3);
        int index = -1;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).equals(user)) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            var result = users.stream()
                    .filter(u -> user.getName().equals(u.getName())
                            && user.getId() != u.getId())
                    .findFirst();
            if (result.isEmpty()) {
                users.remove(index);
                users.add(user);
                logger.info("Updated the user ({}) {}", user.getId(),
                        user.getName());
            } else {
                throw new IllegalArgumentException(
                        "Can't add user with duplicate name");
            }
        } else {
            var result = users.stream()
                    .filter(u -> user.getName().equals(u.getName()))
                    .findFirst();
            if (result.isEmpty()) {
                user.setId(nextUserId);
                nextUserId++;
                users.add(user);
                logger.info("Saved a new user ({}) {}", user.getId(),
                        user.getName());
            } else {
                throw new IllegalArgumentException(
                        "Can't add user with duplicate name");
            }
        }
        return getUserById(user.getId());
    }

    @Override
    public synchronized User getUserById(int userId) {
        randomWait(1);
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == userId) {
                var user = users.get(i);
                return new User(user.getId(), user.getName(), user.getPasswd(),
                        user.getRole());
            }
        }
        return null;
    }

    @Override
    public synchronized void removeUser(int userId) {
        randomWait(1);
        User u = getUserById(userId);
        if (u == null) {
            throw new IllegalArgumentException(
                    "User with id " + userId + " not found");
        }
        logger.info("Removed the user ({}) {}", u.getId(), u.getName());
        users.remove(u);
    }

    @Override
    public synchronized List<User> getAllUsers() {
        randomWait(3);
        var result = new ArrayList<User>();
        for (int i = 0; i < users.size(); i++) {
            result.add(getUserById(users.get(i).getId()));
        }
        return result;
    }

    private void randomWait(int count) {
        int wait = 20 + random.nextInt(30);
        try {
            Thread.sleep(wait * count);
        } catch (InterruptedException e) {
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
