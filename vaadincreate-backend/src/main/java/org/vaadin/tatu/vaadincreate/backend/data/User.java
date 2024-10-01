package org.vaadin.tatu.vaadincreate.backend.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@SuppressWarnings({ "serial", "java:S2160" })
@Entity
@Table(name = "application_user")
public class User extends AbstractEntity {

    public enum Role {
        USER, ADMIN;
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
    private Role role;

    public User() {
        this.name = "";
        this.passwd = "";
        this.role = null;
    }

    public User(Integer id, String name, String passwd, Role role) {
        this.id = id;
        this.name = name;
        this.passwd = passwd;
        this.setRole(role);
    }

    public User(User user) {
        this.id = user.id;
        this.name = user.name;
        this.passwd = user.passwd;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
