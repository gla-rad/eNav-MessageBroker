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
