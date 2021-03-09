package org.grad.eNav.msgBroker.models;

import java.util.Arrays;
import java.util.Objects;

/**
 * The S-100 Node Abstract Class
 *
 * This class implements an abstract object suitable for most S-100 extensions
 * like S-124 and S-125. It will contain a generic representation of the object
 * like a bounding box and the XML object representation.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public abstract class S100AbstractNode implements IJsonSerializable {

    // Class Variables
    private Double[] bbox;
    private String content;

    /**
     * The Default Constructor.
     */
    public S100AbstractNode() {

    }
    /**
     * The Fully Populated Constructor.
     *
     * @param bbox          The object bounding box
     * @param content       The XML content
     */
    public S100AbstractNode(Double[] bbox, String content) {
        this.bbox = bbox;
        this.content = content;
    }

    /**
     * Sets new content.
     *
     * @param content New value of content.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets content.
     *
     * @return Value of content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets bbox.
     *
     * @return Value of bbox.
     */
    public Double[] getBbox() {
        return bbox;
    }

    /**
     * Sets new bbox.
     *
     * @param bbox New value of bbox.
     */
    public void setBbox(Double[] bbox) {
        this.bbox = bbox;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof S100AbstractNode)) return false;
        S100AbstractNode that = (S100AbstractNode) o;
        return Arrays.equals(bbox, that.bbox) && Objects.equals(content, that.content);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = Objects.hash(content);
        result = 31 * result + Arrays.hashCode(bbox);
        return result;
    }

}
