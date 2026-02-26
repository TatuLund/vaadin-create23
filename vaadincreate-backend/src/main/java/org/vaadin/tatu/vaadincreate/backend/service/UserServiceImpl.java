package org.vaadin.tatu.vaadincreate.backend.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.DeputyRequiredException;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.dao.PurchaseDao;
import org.vaadin.tatu.vaadincreate.backend.dao.UserDao;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.backend.mock.MockDataGenerator;

@NullMarked
@SuppressWarnings("java:S6548")
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final PurchaseDao purchaseDao;
    private final Random random;
    private boolean slow = false;

    @Nullable
    private static UserServiceImpl instance;

    @SuppressWarnings("null")
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserServiceImpl();
        }
        return instance;
    }

    private UserServiceImpl() {
        this.userDao = new UserDao();
        this.purchaseDao = new PurchaseDao();
        this.random = new Random();
        var backendMode = System.getProperty("backend.mode");
        if (backendMode != null && backendMode.equals("slow")) {
            slow = true;
        }
        var env = System.getProperty("generate.data");
        if (env == null || env.equals("true")) {
            var users = MockDataGenerator.createUsers();
            users.forEach(user -> userDao.updateUser(user));
            logger.info("Generated mock user data");
        }
    }

    @Override
    public User updateUser(User user) {
        Objects.requireNonNull(user, "User must not be null");
        randomWait(2);
        var existingUser = userDao.findByName(user.getName());
        if (existingUser != null && !existingUser.equals(user)) {
            throw new IllegalArgumentException(
                    "User with the same name already exists");
        }
        return userDao.updateUser(user);
    }

    @Override
    public User updateUser(User editedUser,
            @Nullable User deputyApproverOrNull) {
        Objects.requireNonNull(editedUser, "User must not be null");
        randomWait(2);

        // When activating or no change in active state, use plain update.
        if (editedUser.isActive() && editedUser.getRole() != Role.CUSTOMER) {
            return updateUser(editedUser);
        }

        // User is being deactivated. Validate last-active-admin constraint.
        if (editedUser.getRole() == Role.ADMIN) {
            logger.warn(
                    "Attempted to last active admin deactivation: user {} (id={})",
                    editedUser.getName(), editedUser.getId());
            var existingUser = userDao.getUserById(editedUser.getId());
            if (existingUser != null && existingUser.isActive()
                    && userDao.countActiveAdmins() <= 1) {
                throw new IllegalStateException(
                        "Cannot deactivate the last active admin");
            }
        }

        logger.debug(
                "Counting pending approvals for user {} (id={}) before deactivation",
                editedUser.getName(), editedUser.getId());
        // Check pending purchases, checking for all user types, as Admin
        // may have changed Role to CUSTOMER.
        var pendingCount = purchaseDao.countByApproverAndStatus(editedUser,
                PurchaseStatus.PENDING);
        if (pendingCount > 0L) {
            logger.info("{} pending approvals found for user {} (id={})",
                    pendingCount, editedUser.getName(), editedUser.getId());
            if (deputyApproverOrNull == null) {
                throw new DeputyRequiredException((int) pendingCount);
            }
            validateDeputy(editedUser, deputyApproverOrNull);
            return userDao.deactivateWithReassignment(editedUser,
                    deputyApproverOrNull);
        }

        // No pending approvals; plain update suffices.
        return updateUser(editedUser);
    }

    // Validate that the deputy is active, has an approver role, and is not the
    // same user as the one being deactivated.
    private void validateDeputy(User editedUser, User deputy) {
        Objects.requireNonNull(deputy, "Deputy must not be null");
        logger.debug(
                "Validating deputy approver {} (id={}) for user {} (id={})",
                deputy.getName(), deputy.getId(), editedUser.getName(),
                editedUser.getId());
        var freshDeputy = userDao.getUserById(deputy.getId());
        if (freshDeputy == null || !freshDeputy.isActive()) {
            throw new IllegalArgumentException(
                    "Deputy approver must be active");
        }
        if (freshDeputy.getRole() != User.Role.USER
                && freshDeputy.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Deputy approver must have USER or ADMIN role");
        }
        if (freshDeputy.equals(editedUser)) {
            throw new IllegalArgumentException(
                    "Deputy must not be the same as the edited user");
        }
    }

    @Override
    public Optional<User> findByName(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        randomWait(1);
        User user = userDao.findByName(name);
        return wrapUserInOptional(user);
    }

    @SuppressWarnings("null")
    private Optional<User> wrapUserInOptional(@Nullable User user) {
        return Optional.ofNullable(user);
    }

    @Nullable
    @Override
    public User getUserById(Integer userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        randomWait(1);
        return userDao.getUserById(userId);
    }

    @Override
    public void removeUser(Integer userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        randomWait(1);
        userDao.removeUser(userId);
    }

    @Override
    public List<@NonNull User> getAllUsers() {
        randomWait(3);
        return userDao.getAllUsers();
    }

    @Override
    public List<@NonNull User> getUsersByRole(User.Role role) {
        Objects.requireNonNull(role, "Role must not be null");
        randomWait(2);
        return userDao.getUsersByRole(role);
    }

    @Override
    public List<@NonNull User> getActiveApprovers(User excludeUser) {
        Objects.requireNonNull(excludeUser, "ExcludeUser must not be null");
        randomWait(1);
        return userDao.getActiveApprovers(excludeUser);
    }

    @SuppressWarnings("java:S2142")
    private void randomWait(int count) {
        if (!slow) {
            return;
        }
        int wait = 10 + random.nextInt(20);
        try {
            Thread.sleep(wait * (long) count);
        } catch (InterruptedException e) {
            // NOP
        }
    }

    @SuppressWarnings("null")
    private Logger logger = LoggerFactory.getLogger(this.getClass());

}
