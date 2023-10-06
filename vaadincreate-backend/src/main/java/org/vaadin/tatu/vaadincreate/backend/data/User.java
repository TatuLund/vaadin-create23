package org.vaadin.tatu.vaadincreate.backend.data;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
public class User implements Serializable {

    public enum Role {
        USER, ADMIN;
    }

    private int id;
    private String name;
    private String passwd;
    private Role role;

    public User(int id, String name, String passwd, Role role) {
        super();
        this.id = id;
        this.name = name;
        this.passwd = passwd;
        this.setRole(role);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        return id == other.id;
    }
}
