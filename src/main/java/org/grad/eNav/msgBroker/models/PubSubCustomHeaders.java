package org.grad.eNav.msgBroker.models;

/**
 * The PubSubCustomHeaders Class
 *
 * This is a class that defines the additional custom headers for the S-124 and
 * S-125 custom headers of the publish subscribe internal messages of the
 * broker micro-service.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class PubSubCustomHeaders {

    // The Custom Headers
    public static String PUBSUB_S124_ID = "PUBSUB-S124-ID";
    public static String PUBSUB_S125_ID = "PUBSUBS-125-ID";
    public static String PUBSUB_BBOX = "PUBSUB-BBOX";

}
