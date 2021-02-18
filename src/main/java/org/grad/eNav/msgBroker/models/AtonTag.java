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

/**
 * The AtoN Tag Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class AtonTag implements IJsonSerializable {

    // Custom AtoN tags
    public static final String TAG_ATON_UID         = "seamark:ref";
    public static final String TAG_LIGHT_NUMBER     = "seamark:light:ref";
    public static final String TAG_INT_LIGHT_NUMBER = "seamark:light:int_ref";
    public static final String TAG_LOCALITY         = "seamark:locality";
    public static final String TAG_AIS_NUMBER       = "seamark:ais:ref";
    public static final String TAG_RACON_NUMBER     = "seamark:racon:ref";
    public static final String TAG_INT_RACON_NUMBER = "seamark:racon:int_ref";

    // Class Variables
    String k;
    String v;

    /** No-arg constructor */
    public AtonTag() {
    }

    /** Key-value constructor */
    public AtonTag(String k, String v) {
        this.k = k;
        this.v = v;
    }

    /**
     * Sets new k.
     *
     * @param k New value of k.
     */
    public void setK(String k) {
        this.k = k;
    }

    /**
     * Gets k.
     *
     * @return Value of k.
     */
    public String getK() {
        return k;
    }

    /**
     * Gets v.
     *
     * @return Value of v.
     */
    public String getV() {
        return v;
    }

    /**
     * Sets new v.
     *
     * @param v New value of v.
     */
    public void setV(String v) {
        this.v = v;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("all")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AtonTag atonTag = (AtonTag) o;

        if (k != null ? !k.equals(atonTag.k) : atonTag.k != null) return false;
        return v != null ? v.equals(atonTag.v) : atonTag.v == null;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = k != null ? k.hashCode() : 0;
        result = 31 * result + (v != null ? v.hashCode() : 0);
        return result;
    }
}
