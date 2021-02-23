package org.grad.eNav.msgBroker.models;

/**
 * The Publication Type Enum.
 *
 * This enumeration is used to define all the publication types currenlty
 * supported by the message broker micro-service.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public enum PublicationType {
    ATON("aton");

    /**
     * The Publication Type string representation.
     */
    private String type;

    /**
     * The Publication Type Constructor.
     *
     * @param type      The string representation of the type
     */
    PublicationType(String type) {
        this.type = type;
    }

    /**
     * Gets type.
     *
     * @return Value of type.
     */
    public String getType() {
        return type;
    }
}
