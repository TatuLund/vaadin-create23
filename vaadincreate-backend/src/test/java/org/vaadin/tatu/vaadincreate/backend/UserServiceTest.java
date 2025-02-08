package org.vaadin.tatu.vaadincreate.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.persistence.OptimisticLockException;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.backend.service.UserServiceImpl;

public class UserServiceTest {

    private UserService service;

    @Before
    public void setUp() {
        service = UserServiceImpl.getInstance();
    }

    @Test
    public void canFetchUsers() {
        assertFalse(service.getAllUsers().isEmpty());
    }

    @Test
    public void updateTheUser() {
        var oldSize = service.getAllUsers().size();
        var user = service.getAllUsers().iterator().next();
        var version = user.getVersion();
        user.setName("Test1");
        User user2 = service.updateUser(user);
        assertEquals((Integer.valueOf(version + 1)), user2.getVersion());
        assertEquals("Test1", user2.getName());
        assertEquals(oldSize, service.getAllUsers().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateTheUserByDuplicateName() {
        var user = service.getAllUsers().iterator().next();
        user.setName("Admin");
        service.updateUser(user);
    }

    @Test(expected = OptimisticLockException.class)
    public void optimisticLocking() {
        var user = service.findByName("User5").get();
        var copy = new User(user);
        service.updateUser(user);
        service.updateUser(copy);
    }

    @Test
    public void addNewUser() {
        var oldSize = service.getAllUsers().size();
        User user = new User();
        user.setName("Test2");
        user.setPasswd("test2");
        user.setRole(Role.USER);
        var newUser = service.updateUser(user);
        assertEquals(Integer.valueOf(0), newUser.getVersion());
        assertEquals(oldSize + 1, service.getAllUsers().size());

        var foundUser = service.findByName("Test2");
        assertTrue(foundUser.get().equals(newUser));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNewUserByDuplicateName() {
        User user = new User();
        user.setName("Admin");
        user.setPasswd("admin");
        user.setRole(Role.USER);
        service.updateUser(user);
    }

    @Test
    public void removeUser() {
        var oldSize = service.getAllUsers().size();
        var u = service.getAllUsers().iterator().next();
        var uid = u.getId();
        service.removeUser(uid);
        assertEquals(null, service.getUserById(uid));
        assertEquals(oldSize - 1, service.getAllUsers().size());
    }

    @Test
    public void findUserById() {
        var user = service.getAllUsers().get(0);
        assertNotNull(service.getUserById(user.getId()));
    }

    @Test
    public void findUserByNonExistentId() {
        assertEquals(null, service.getUserById(1000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeUserByNonExistentId() {
        service.removeUser(1000);
    }

}
