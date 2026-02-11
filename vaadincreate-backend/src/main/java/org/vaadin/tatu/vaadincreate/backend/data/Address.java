package org.vaadin.tatu.vaadincreate.backend.data;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

import org.jspecify.annotations.NullMarked;

/**
 * Value object representing a physical address.
 * This is used as an embedded object in entities to store address information.
 */
@NullMarked
@Embeddable
@SuppressWarnings("serial")
public class Address implements Serializable {

    @NotNull(message = "{address.street.required}")
    @Column(name = "street")
    private String street = "";

    @NotNull(message = "{address.postalCode.required}")
    @Column(name = "postal_code")
    private String postalCode = "";

    @NotNull(message = "{address.city.required}")
    @Column(name = "city")
    private String city = "";

    @NotNull(message = "{address.country.required}")
    @Column(name = "country")
    private String country = "";

    /**
     * Default constructor.
     */
    public Address() {
    }

    /**
     * Constructs an Address with all fields.
     *
     * @param street the street address
     * @param postalCode the postal code
     * @param city the city name
     * @param country the country name
     */
    public Address(String street, String postalCode, String city, String country) {
        this.street = Objects.requireNonNull(street, "Street must not be null");
        this.postalCode = Objects.requireNonNull(postalCode, "Postal code must not be null");
        this.city = Objects.requireNonNull(city, "City must not be null");
        this.country = Objects.requireNonNull(country, "Country must not be null");
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(street, address.street) &&
                Objects.equals(postalCode, address.postalCode) &&
                Objects.equals(city, address.city) &&
                Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, postalCode, city, country);
    }

    @Override
    public String toString() {
        return street + ", " + postalCode + " " + city + ", " + country;
    }
}
