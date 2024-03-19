package org.vaadin.tatu.vaadincreate.admin;

public interface TabView {

    /**
     * Returns the name of the tab.
     *
     * @return the name of the tab as a String
     */
    public String getTabName();

    /**
     * Method to be called when the tab is entered.
     */
    public void enter();

}
