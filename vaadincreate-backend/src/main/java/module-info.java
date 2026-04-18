module org.vaadin.tatu.vaadincreate.backend {
    requires java.sql;
    requires java.naming;

    requires org.slf4j;
    requires org.hibernate.orm.core;
    requires redis.clients.jedis;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    requires static java.persistence;
    requires static java.validation;
    requires static javax.servlet.api;
    requires static org.jspecify;

    exports org.vaadin.tatu.vaadincreate.backend;
    exports org.vaadin.tatu.vaadincreate.backend.data;
    exports org.vaadin.tatu.vaadincreate.backend.events;

    opens org.vaadin.tatu.vaadincreate.backend.data
            to org.hibernate.orm.core, com.fasterxml.jackson.databind;
}