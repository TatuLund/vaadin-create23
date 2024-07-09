package org.vaadin.tatu.vaadincreate.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import javax.persistence.OptimisticLockException;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.backend.mock.MockUserService;

public class UserServiceTest {

    private UserService service;

    @Before
    public void setUp() throws Exception {
        service = MockUserService.getInstance();
    }

    @Test
    public void canFetchUsers() throws Exception {
        assertFalse(service.getAllUsers().isEmpty());
    }

    @Test
    public void updateTheUser() throws Exception {
        var oldSize = service.getAllUsers().size();
        var user = service.getAllUsers().iterator().next();
        var version = user.getVersion();
        user.setName("Test1");
        User user2 = service.updateUser(user);
        assertEquals(version + 1, user2.getVersion());
        assertEquals("Test1", user2.getName());
        assertEquals(oldSize, service.getAllUsers().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateTheUserByDuplicateName() throws Exception {
        var user = service.getAllUsers().iterator().next();
        user.setName("Admin");
        service.updateUser(user);
    }

    @Test(expected = OptimisticLockException.class)
    public void optimisticLocking() {
        var user = service.findByName("User5").get();
        var u = service.updateUser(user);
        service.updateUser(user);
    }

    @Test
    public void addNewUser() throws Exception {
        var oldSize = service.getAllUsers().size();
        User user = new User(20, "Test2", "test2", Role.USER, 0);
        var newUser = service.updateUser(user);
        assertEquals(0, newUser.getVersion());
        assertEquals(oldSize + 1, service.getAllUsers().size());

        var foundUser = service.findByName("Test2");
        assertTrue(foundUser.get().equals(newUser));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNewUserByDuplicateName() throws Exception {
        User user = new User(20, "Admin", "admin", Role.USER, 0);
        service.updateUser(user);
    }

    @Test
    public void removeUser() throws Exception {
        var oldSize = service.getAllUsers().size();
        var u = service.getAllUsers().iterator().next();
        var uid = u.getId();
        service.removeUser(uid);
        assertEquals(null, service.getUserById(uid));
        assertEquals(oldSize - 1, service.getAllUsers().size());
    }

    @Test
    public void findUserById() {
        assertNotEquals(null, service.getUserById(1));
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
