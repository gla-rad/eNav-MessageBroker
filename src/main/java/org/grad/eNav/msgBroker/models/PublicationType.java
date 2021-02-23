package org.grad.eNav.msgBroker.models;

/**
 * The Publication Type Enum.
 *
 * This enumeration is used to define all the publication types currently
 * supported by the message broker micro-service.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public enum PublicationType {
    ATON("aton"),
    NAVIGATION_MESSAGE("navigation-message"),
    NAVIGATION_WARNING("navigation-warning");

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
