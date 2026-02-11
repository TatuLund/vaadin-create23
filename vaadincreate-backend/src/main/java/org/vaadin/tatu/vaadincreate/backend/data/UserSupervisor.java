package org.vaadin.tatu.vaadincreate.backend.data;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.jspecify.annotations.NullMarked;

/**
 * Entity representing the mapping between an employee and their default supervisor.
 * This is used to automatically assign an approver when a purchase request is created.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
@Table(name = "user_supervisor")
public class UserSupervisor extends AbstractEntity {

    @NotNull(message = "{employee.required}")
    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @NotNull(message = "{supervisor.required}")
    @ManyToOne(optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;

    /**
     * Default constructor.
     */
    public UserSupervisor() {
    }

    /**
     * Constructs a UserSupervisor mapping.
     *
     * @param employee the employee (typically with CUSTOMER role)
     * @param supervisor the supervisor (typically with USER or ADMIN role)
     */
    public UserSupervisor(User employee, User supervisor) {
        this.employee = Objects.requireNonNull(employee, "Employee must not be null");
        this.supervisor = Objects.requireNonNull(supervisor, "Supervisor must not be null");
    }

    public User getEmployee() {
        return employee;
    }

    public void setEmployee(User employee) {
        this.employee = employee;
    }

    public User getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(User supervisor) {
        this.supervisor = supervisor;
    }
}
