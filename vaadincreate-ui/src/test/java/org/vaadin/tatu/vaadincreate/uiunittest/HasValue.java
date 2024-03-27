package org.vaadin.tatu.vaadincreate.uiunittest;

public interface HasValue<T> {

    /**
     * Set value as user. This will mean that accompanying event will have
     * isUserOriginated = true.
     * 
     * @param value
     *            The value
     */
    public void setValue(T value);

}
