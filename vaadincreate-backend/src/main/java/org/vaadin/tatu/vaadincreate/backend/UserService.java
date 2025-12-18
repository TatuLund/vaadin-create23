package org.vaadin.tatu.vaadincreate.backend;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.service.UserServiceImpl;

@NullMarked
public interface UserService {

    public abstract Optional<User> findByName(String name);

    public abstract User updateUser(User user);

    public abstract List<@NonNull User> getAllUsers();

    @Nullable
    public User getUserById(Integer userId);

    void removeUser(Integer userId);

    public static UserService get() {
        return UserServiceImpl.getInstance();
    }

}
