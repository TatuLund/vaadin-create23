package org.vaadin.tatu.vaadincreate.backend.data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@SuppressWarnings("serial")
public class User extends AbstractEntity {

    public enum Role {
        USER, ADMIN;
    }

    @NotNull(message = "{user.name.required}")
    @Size(min = 5, max = 20, message = "{user.length}")
    private String name;

    @NotNull(message = "{passwd.required}")
    @Size(min = 5, max = 20, message = "{passwd.length}")
    private String passwd;

    @NotNull(message = "{role.required}")
    private Role role;

    public User() {
        this.id = -1;
        this.name = "";
        this.passwd = "";
        this.role = null;
    }

    public User(int id, String name, String passwd, Role role) {
        this.id = id;
        this.name = name;
        this.passwd = passwd;
        this.setRole(role);
        this.version = 0;
    }

    public User(int id, String name, String passwd, Role role, int version) {
        this.id = id;
        this.name = name;
        this.passwd = passwd;
        this.setRole(role);
        this.version = version;
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
