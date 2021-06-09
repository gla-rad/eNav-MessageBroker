/*
 * Copyright (c) 2021 GLA UK Research and Development Directive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.grad.eNav.msgBroker.models;

import com.fasterxml.jackson.databind.JsonNode;

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
    private JsonNode bbox;
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
    public S100AbstractNode(JsonNode bbox, String content) {
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
    public JsonNode getBbox() {
        return bbox;
    }

    /**
     * Sets new bbox.
     *
     * @param bbox New value of bbox.
     */
    public void setBbox(JsonNode bbox) {
        this.bbox = bbox;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S100AbstractNode that = (S100AbstractNode) o;
        return Objects.equals(bbox, that.bbox) && Objects.equals(content, that.content);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(bbox, content);
    }
}
