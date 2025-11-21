/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.msgBroker.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * The S125 Node Class.
 *
 * This node extends the S-100 abstract node to implement the S-125 messages
 * including the S-125 UID value.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class S125Node extends S100AbstractNode {

    // Class Variables
    private String datasetUID;

    /**
     * The Fully Populated  Constructor.
     *
     * @param datasetUID       The AtoN UID
     * @param geometry      The object geometry
     * @param content       The XML content
     */
    public S125Node(String datasetUID, JsonNode geometry, String content) {
        super(geometry, content);
        this.datasetUID = datasetUID;
    }

    /**
     * Sets new atonUID.
     *
     * @param datasetUID New value of atonUID.
     */
    public void setDatasetUID(String datasetUID) {
        this.datasetUID = datasetUID;
    }

    /**
     * Gets atonUID.
     *
     * @return Value of atonUID.
     */
    public String getDatasetUID() {
        return datasetUID;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof S125Node)) return false;
        if (!super.equals(o)) return false;
        S125Node s125Node = (S125Node) o;
        return Objects.equals(datasetUID, s125Node.datasetUID);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), datasetUID);
    }

}
