package org.vaadin.tatu.vaadincreate.backend;

import java.util.List;
import java.util.Optional;

import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.service.UserServiceImpl;

public interface UserService {

    public abstract Optional<User> findByName(String name);

    public abstract User updateUser(User user);

    public abstract List<User> getAllUsers();

    public User getUserById(Integer userId);

    void removeUser(Integer userId);

    public static UserService get() {
        return UserServiceImpl.getInstance();
    }

}
