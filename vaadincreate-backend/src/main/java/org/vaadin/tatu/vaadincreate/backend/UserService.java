package org.vaadin.tatu.vaadincreate.backend;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.mock.MockUserService;

public interface UserService extends Serializable {

    public abstract Optional<User> findByName(String name);

    public abstract User updateUser(User user);

    public abstract List<User> getAllUsers();

    public static UserService get() {
        return MockUserService.getInstance();
    }

    public User getUserById(int userId);

    void removeUser(int userId);

}
