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

    private MockUserService() {
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
                    user.getPasswd(), user.getRole(), user.getVersion()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public synchronized User updateUser(User user) {
        randomWait(3);
        User newUser = null;
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
                newUser = new User(user.getId(), user.getName(),
                        user.getPasswd(), user.getRole(), user.getVersion());
                newUser.setVersion(users.get(index).getVersion() + 1);
                users.set(index, newUser);
                logger.info("Updated the user ({}) {}", newUser.getId(),
                        newUser.getName());
            } else {
                throw new IllegalArgumentException(
                        "Can't add user with duplicate name");
            }
        } else {
            var result = users.stream()
                    .filter(u -> user.getName().equals(u.getName()))
                    .findFirst();
            if (result.isEmpty()) {
                newUser = new User(-1, user.getName(), user.getPasswd(),
                        user.getRole(), 0);
                newUser.setId(nextUserId);
                nextUserId++;
                users.add(newUser);
                logger.info("Saved a new user ({}) {}", newUser.getId(),
                        newUser.getName());
            } else {
                throw new IllegalArgumentException(
                        "Can't add user with duplicate name");
            }
        }
        return newUser;
    }

    @Override
    public synchronized User getUserById(int userId) {
        randomWait(1);
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == userId) {
                var user = users.get(i);
                return new User(user.getId(), user.getName(), user.getPasswd(),
                        user.getRole(), user.getVersion());
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
