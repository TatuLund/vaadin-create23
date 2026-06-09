package org.vaadin.tatu.vaadincreate.storefront;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.data.Address;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.data.Binder;
import com.vaadin.ui.FormLayout;

@NullMarked
@SuppressWarnings({ "java:S110", "java:S2160" })
class AddressForm extends FormLayout
        implements HasAttributes<AddressForm>, HasI18N {

    private final Binder<Address> binder;

    /**
     * Creates an address form bound to the given Address bean. The form
     * includes fields for street, postal code, city, and country, all of which
     * are required.
     *
     * @param address
     *            the Address bean to bind to the form
     */
    public AddressForm(Address address) {
        binder = new Binder<>(Address.class);
        setRole("form");

        var streetField = new ATextField(
                getTranslation(I18n.Storefront.STREET));
        streetField.setWidth("100%");
        streetField.setAutocomplete("street-address");
        binder.forField(streetField)
                .asRequired(getTranslation(
                        I18n.Storefront.STREET_REQUIRED))
                .bind(Address::getStreet, Address::setStreet);

        var postalCodeField = new ATextField(
                getTranslation(I18n.Storefront.POSTAL_CODE));
        postalCodeField.setAutocomplete("postal-code");
        binder.forField(postalCodeField)
                .asRequired(getTranslation(
                        I18n.Storefront.POSTAL_CODE_REQUIRED))
                .bind(Address::getPostalCode, Address::setPostalCode);

        var cityField = new ATextField(
                getTranslation(I18n.Storefront.CITY));
        cityField.setAutocomplete("address-level2");
        binder.forField(cityField)
                .asRequired(getTranslation(I18n.Storefront.CITY_REQUIRED))
                .bind(Address::getCity, Address::setCity);

        var countryField = new ATextField(
                getTranslation(I18n.Storefront.COUNTRY));
        countryField.setAutocomplete("country");
        binder.forField(countryField)
                .asRequired(getTranslation(
                        I18n.Storefront.COUNTRY_REQUIRED))
                .bind(Address::getCountry, Address::setCountry);

        binder.setBean(address);
        addComponents(streetField, postalCodeField, cityField,
                countryField);
        streetField.focus();
    }

    /**
     * Returns the Binder used for this address form, allowing the caller to
     * write the form data back to the Address bean and check for validation.
     */
    public Binder<Address> getBinder() {
        return binder;
    }
}
