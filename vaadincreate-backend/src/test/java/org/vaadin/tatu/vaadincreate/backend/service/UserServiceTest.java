package org.vaadin.tatu.vaadincreate.backend.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.persistence.OptimisticLockException;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.DeputyRequiredException;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.backend.data.Cart;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

/**
 * Test class for {@link UserService}.
 *
 * These integration tests run against the in-memory H2 database.
 */
@SuppressWarnings("null")
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
        var u = service.getAllUsers().get(oldSize - 1);
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

    @Test
    public void getUsersByRole() {
        var userRoleUsers = service.getUsersByRole(Role.USER);
        assertFalse(userRoleUsers.isEmpty());
        assertTrue(
                userRoleUsers.stream().allMatch(u -> u.getRole() == Role.USER));

        var adminUsers = service.getUsersByRole(Role.ADMIN);
        assertFalse(adminUsers.isEmpty());
        assertTrue(
                adminUsers.stream().allMatch(u -> u.getRole() == Role.ADMIN));

        var customerUsers = service.getUsersByRole(Role.CUSTOMER);
        assertFalse(customerUsers.isEmpty());
        assertTrue(customerUsers.stream()
                .allMatch(u -> u.getRole() == Role.CUSTOMER));
    }

    @Test
    public void newUserIsActiveByDefault() {
        var user = new User();
        user.setName("ActiveUser");
        user.setPasswd("password");
        user.setRole(Role.USER);
        var saved = service.updateUser(user);
        assertTrue("New user should be active by default", saved.isActive());
    }

    @Test
    public void deactivateUserWithNoPendingApprovals() {
        // User3 is a USER role user with no pending approvals
        var user = service.findByName("User3").get();
        assertTrue(user.isActive());
        user.setActive(false);
        var updated = service.updateUser(user, null);
        assertFalse("User should be deactivated", updated.isActive());
        // Restore for other tests
        updated = service.getUserById(updated.getId());
        updated.setActive(true);
        service.updateUser(updated, null);
    }

    @Test
    public void deactivateCustomerWithNoApprovals() {
        var customer = service.findByName("Customer0").get();
        assertTrue(customer.isActive());
        customer.setActive(false);
        var updated = service.updateUser(customer, null);
        assertFalse("Customer should be deactivated", updated.isActive());
        // Restore
        updated = service.getUserById(updated.getId());
        updated.setActive(true);
        service.updateUser(updated, null);
    }

    @Test(expected = DeputyRequiredException.class)
    public void deactivateApproverWithPendingApprovalsAndNoDeputy() {
        // Initialize ProductDataService before PurchaseService (required for
        // mock data generation)
        var productService = ProductDataServiceImpl.getInstance();
        var purchaseService = PurchaseServiceImpl.getInstance();
        var approver = service.findByName("User7").get();
        var customer = service.findByName("Customer30").get();
        var product = productService.getAllProducts().iterator().next();
        var cart = new Cart();
        cart.addItem(product, 1);
        var address = new Address("Test St 1", "12345", "TestCity", "Finland");
        purchaseService.createPendingPurchase(cart, address, customer,
                approver);

        // Deactivate User7 without a deputy – must throw
        // DeputyRequiredException
        approver.setActive(false);
        service.updateUser(approver, null);
    }

    @Test
    public void deactivateApproverWithPendingApprovalsWithDeputy() {
        var productService = ProductDataServiceImpl.getInstance();
        var purchaseService = PurchaseServiceImpl.getInstance();
        var approver = service.findByName("User4").get();
        var deputy = service.findByName("User2").get();
        var customer = service.findByName("Customer40").get();
        var product = productService.getAllProducts().iterator().next();
        var cart = new Cart();
        cart.addItem(product, 1);
        var address = new Address("Deputy St 1", "54321", "DeputyCity",
                "Finland");
        purchaseService.createPendingPurchase(cart, address, customer,
                approver);

        long pendingBefore = purchaseService.countPendingForApprover(approver);
        assertTrue("Should have pending approvals", pendingBefore > 0);

        approver.setActive(false);
        var updated = service.updateUser(approver, deputy);
        assertFalse("User should be deactivated", updated.isActive());

        // Pending approvals for the original approver should be reassigned
        assertEquals("Approver should have no more pending after reassignment",
                0L, purchaseService.countPendingForApprover(updated));
        assertTrue("Deputy should have the reassigned pending approvals",
                purchaseService
                        .countPendingForApprover(deputy) >= pendingBefore);

        // Restore
        updated.setActive(true);
        service.updateUser(updated, null);
    }

    @Test(expected = IllegalStateException.class)
    public void cannotDeactivateLastActiveAdmin() {
        // There are two admins (Admin and Super). Deactivate one first.
        var admin1 = service.findByName("Admin").get();
        var admin2 = service.findByName("Super").get();

        // Deactivate admin1
        admin1.setActive(false);
        service.updateUser(admin1, null);

        try {
            // Now admin2 is the last active admin – deactivating them must fail
            admin2.setActive(false);
            service.updateUser(admin2, null);
        } finally {
            // Restore admin1
            admin1 = service.findByName("Admin").get();
            admin1.setActive(true);
            service.updateUser(admin1, null);
        }
    }

    @Test
    public void getActiveApproversExcludesEditedUserAndInactive() {
        var user = service.findByName("User0").get();
        var approvers = service.getActiveApprovers(user);
        assertFalse("Should have active approvers", approvers.isEmpty());
        assertTrue("Edited user must not appear in deputy list",
                approvers.stream().noneMatch(u -> u.equals(user)));
        assertTrue("All deputy candidates must be active",
                approvers.stream().allMatch(User::isActive));
        assertTrue(
                "All deputy candidates must have USER or ADMIN role",
                approvers.stream().allMatch(u -> u.getRole() == Role.USER
                        || u.getRole() == Role.ADMIN));
    }

}
