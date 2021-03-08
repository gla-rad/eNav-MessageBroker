package org.grad.eNav.msgBroker.models;

import java.util.Objects;

/**
 * The S125 Node Class.
 *
 * This node extends the S-100 abstract node to implement the S-125 messages
 * including the AtoN UID value.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class S125Node extends S100AbstractNode {

    // Class Variables
    public String atonUID;

    /**
     * The Fully Populated  Constructor.
     *
     * @param atonUID       The AtoN UID
     * @param bbox          The object bounding box
     * @param content       The XML content
     */
    public S125Node(String atonUID, Double[] bbox, String content) {
        super(bbox, content);
        this.atonUID = atonUID;
    }

    /**
     * Sets new atonUID.
     *
     * @param atonUID New value of atonUID.
     */
    public void setAtonUID(String atonUID) {
        this.atonUID = atonUID;
    }

    /**
     * Gets atonUID.
     *
     * @return Value of atonUID.
     */
    public String getAtonUID() {
        return atonUID;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof S125Node)) return false;
        if (!super.equals(o)) return false;
        S125Node s125Node = (S125Node) o;
        return Objects.equals(atonUID, s125Node.atonUID);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), atonUID);
    }

}
