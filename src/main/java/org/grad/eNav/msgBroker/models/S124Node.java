package org.grad.eNav.msgBroker.models;

import java.util.Objects;

/**
 * The S124 Node Class.
 *
 * This node extends the S-100 abstract node to implement the S-124 messages
 * including the Message ID value.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class S124Node extends S100AbstractNode {

    // Class Variables
    private String messageId;

    /**
     * The Fully Populated  Constructor.
     *
     * @param messageId     The Message ID
     * @param bbox          The object bounding box
     * @param content       The XML content
     */
    public S124Node(String messageId, Double[] bbox, String content) {
        super(bbox, content);
        this.messageId = messageId;
    }

    /**
     * Sets new messageId.
     *
     * @param messageId New value of messageId.
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Gets messageId.
     *
     * @return Value of messageId.
     */
    public String getMessageId() {
        return messageId;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof S124Node)) return false;
        if (!super.equals(o)) return false;
        S124Node s125Node = (S124Node) o;
        return Objects.equals(messageId, s125Node.messageId);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messageId);
    }

}
