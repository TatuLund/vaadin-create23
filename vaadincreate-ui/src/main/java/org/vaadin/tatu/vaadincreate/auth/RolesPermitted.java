package org.vaadin.tatu.vaadincreate.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

/**
 * Defines roles that are permitted to access the view.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RolesPermitted {

    Role[] value();
}
