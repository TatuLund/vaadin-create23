package org.vaadin.tatu.vaadincreate.backend.data;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
@Table(name = "application_user")
public class User extends AbstractEntity {

    /**
     * Enum representing the roles a user can have in the system.
     * <p>
     * The roles available are:
     * <ul>
     * <li>{@link #CUSTOMER} - Customer role for employees who create purchase
     * requests.</li>
     * <li>{@link #USER} - Regular user with standard permissions, can act as
     * supervisor.</li>
     * <li>{@link #ADMIN} - Administrator with elevated permissions.</li>
     * </ul>
     */
    public enum Role {
        CUSTOMER, USER, ADMIN;
    }

    @NotNull(message = "{user.name.required}")
    @Size(min = 5, max = 20, message = "{user.length}")
    @Column(name = "user_name")
    private String name;

    @NotNull(message = "{passwd.required}")
    @Size(min = 5, max = 20, message = "{passwd.length}")
    @Column(name = "passwd")
    private String passwd;

    @NotNull(message = "{role.required}")
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Nullable
    private Role role;

    public User() {
        this.name = "";
        this.passwd = "";
        this.role = null;
    }

    /**
     * Constructs a new User with the specified id, name, password, and role.
     *
     * @param id
     *            the unique identifier for the user, must not be null
     * @param name
     *            the name of the user, must not be null
     * @param passwd
     *            the password of the user, must not be null
     * @param role
     *            the role of the user, must not be null
     */
    public User(Integer id, String name, String passwd, Role role) {
        this.id = id;
        this.name = name;
        this.passwd = passwd;
        this.setRole(role);
    }

    /**
     * Copy constructor for creating a new User instance by copying the fields
     * from an existing User instance.
     *
     * @param user
     *            the User instance to copy from
     * @throws NullPointerException
     *             if the provided user is null
     */
    public User(User user) {
        Objects.requireNonNull(user, "User to copy must not be null");
        this.id = user.id;
        this.name = user.name;
        this.passwd = user.passwd;
        assert user.role != null;
        this.setRole(user.role);
        this.version = user.version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    @Nullable
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
