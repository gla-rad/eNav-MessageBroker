/*
 * Copyright (c) 2021 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.msgBroker.models;

import java.util.List;
import java.util.Objects;

/**
 * The Search Result Class containing the search output.
 *
 * @param <T>       The search result data class type
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class SearchResult<T> implements IJsonSerializable {

    private List<T> data;
    private int total;
    private int size;

    /**
     * Gets size.
     *
     * @return Value of size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets new size.
     *
     * @param size New value of size.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Gets total.
     *
     * @return Value of total.
     */
    public int getTotal() {
        return total;
    }

    /**
     * Sets new total.
     *
     * @param total New value of total.
     */
    public void setTotal(int total) {
        this.total = total;
    }

    /**
     * Sets new data.
     *
     * @param data New value of data.
     */
    public void setData(List<T> data) {
        this.data = data;
    }

    /**
     * Gets data.
     *
     * @return Value of data.
     */
    public List<T> getData() {
        return data;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchResult)) return false;
        SearchResult<?> that = (SearchResult<?>) o;
        return total == that.total && size == that.size && Objects.equals(data, that.data);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(data, total, size);
    }
}
