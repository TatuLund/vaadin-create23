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

    /**
     * Updates a user, handling the deactivation-with-reassignment flow
     * atomically. When {@code editedUser.active == true} this behaves like the
     * plain {@link #updateUser(User)} overload and ignores
     * {@code deputyApproverOrNull}.
     *
     * <p>
     * When {@code editedUser.active == false}:
     * <ul>
     * <li>If there are PENDING purchases assigned to this user as approver
     * <strong>and</strong> {@code deputyApproverOrNull == null}, a
     * {@link DeputyRequiredException} is thrown with the pending count.</li>
     * <li>If there are PENDING purchases and a deputy is supplied, both the
     * reassignment and the user update are performed in a single
     * transaction.</li>
     * <li>Deactivating the last active {@code ADMIN} throws
     * {@link IllegalStateException}.</li>
     * </ul>
     *
     * @param editedUser
     *            the user to persist (with new field values including
     *            {@code active})
     * @param deputyApproverOrNull
     *            active USER/ADMIN to reassign pending purchases to, or
     *            {@code null} when no pending purchases exist
     * @return the persisted user
     * @throws DeputyRequiredException
     *             if {@code editedUser.active == false} and there are pending
     *             approvals but no deputy was supplied
     * @throws IllegalStateException
     *             if the operation would deactivate the last active ADMIN
     * @throws IllegalArgumentException
     *             if the deputy is invalid (inactive, wrong role, or same as
     *             edited user)
     */
    public abstract User updateUser(User editedUser,
            @Nullable User deputyApproverOrNull);

    public abstract List<@NonNull User> getAllUsers();

    public abstract List<@NonNull User> getUsersByRole(User.Role role);

    @Nullable
    public User getUserById(Integer userId);

    void removeUser(Integer userId);

    /**
     * Returns all active users with role {@code USER} or {@code ADMIN},
     * excluding the given user. Used to populate the deputy-approver ComboBox.
     *
     * @param excludeUser
     *            the user to exclude (the one being edited)
     * @return list of eligible deputy approvers
     */
    public List<@NonNull User> getActiveApprovers(User excludeUser);

    public static UserService get() {
        return UserServiceImpl.getInstance();
    }

}
