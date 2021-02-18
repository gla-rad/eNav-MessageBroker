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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.StringUtils;

import java.beans.Transient;
import java.util.*;

/**
 * The AtoN Node Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class AtonNode implements IJsonSerializable {

    // Class Variables
    private Integer id;
    private Double lat;
    private Double lon;
    private String user;
    private Integer uid;
    private Boolean visible;
    private Integer version;
    private Integer changeset;
    private Date timestamp;
    @JsonProperty("tags")
    @JsonSerialize(using = AtonTagJsonSerialization.Serializer.class)
    @JsonDeserialize(using = AtonTagJsonSerialization.Deserializer.class)
    private AtonTag[] tags;

    /**
     * Gets timestamp.
     *
     * @return Value of timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Gets user.
     *
     * @return Value of user.
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets new tags.
     *
     * @param tags New value of tags.
     */
    public void setTags(AtonTag[] tags) {
        this.tags = tags;
    }

    /**
     * Sets new uid.
     *
     * @param uid New value of uid.
     */
    public void setUid(Integer uid) {
        this.uid = uid;
    }

    /**
     * Sets new version.
     *
     * @param version New value of version.
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Sets new lon.
     *
     * @param lon New value of lon.
     */
    public void setLon(Double lon) {
        this.lon = lon;
    }

    /**
     * Gets lat.
     *
     * @return Value of lat.
     */
    public Double getLat() {
        return lat;
    }

    /**
     * Sets new timestamp.
     *
     * @param timestamp New value of timestamp.
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets new lat.
     *
     * @param lat New value of lat.
     */
    public void setLat(Double lat) {
        this.lat = lat;
    }

    /**
     * Sets new visible.
     *
     * @param visible New value of visible.
     */
    public void setVisible(Boolean visible) {
        this.visible = null;
    }

    /**
     * Gets id.
     *
     * @return Value of id.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets new changeset.
     *
     * @param changeset New value of changeset.
     */
    public void setChangeset(Integer changeset) {
        this.changeset = changeset;
    }

    /**
     * Gets tags.
     *
     * @return Value of tags.
     */
    public AtonTag[] getTags() {
        return tags;
    }

    /**
     * Gets uid.
     *
     * @return Value of uid.
     */
    public Integer getUid() {
        return uid;
    }

    /**
     * Sets new id.
     *
     * @param id New value of id.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Gets version.
     *
     * @return Value of version.
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Gets changeset.
     *
     * @return Value of changeset.
     */
    public Integer getChangeset() {
        return changeset;
    }

    /**
     * Gets visible.
     *
     * @return Value of visible.
     */
    public Boolean getVisible() {
        return visible;
    }

    /**
     * Gets lon.
     *
     * @return Value of lon.
     */
    public Double getLon() {
        return lon;
    }

    /**
     * Sets new user.
     *
     * @param user New value of user.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtonNode)) return false;
        AtonNode atonNode = (AtonNode) o;
        return Objects.equals(id, atonNode.id) && Objects.equals(lat, atonNode.lat) && Objects.equals(lon, atonNode.lon) && Objects.equals(user, atonNode.user) && Objects.equals(uid, atonNode.uid) && Objects.equals(visible, atonNode.visible) && Objects.equals(version, atonNode.version) && Objects.equals(changeset, atonNode.changeset) && Objects.equals(timestamp, atonNode.timestamp) && Arrays.equals(tags, atonNode.tags);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = Objects.hash(id, lat, lon, user, uid, visible, version, changeset, timestamp);
        result = 31 * result + Arrays.hashCode(tags);
        return result;
    }

    /**
     * Returns the AtoN UID
     * @return the AtoN UID
     */
    @Transient
    public String getAtonUid() {
        return getTagValue(AtonTag.TAG_ATON_UID);
    }

    /**
     * Returns the value of the tag with the given key. Returns null if the tag does not exist
     * @param k the key
     * @return the value of the tag with the given key. Returns null if the tag does not exist
     */
    @Transient
    public String getTagValue(String k) {
        AtonTag atonUidTag = getTag(k);
        return atonUidTag == null ? null : atonUidTag.getV();
    }

    /**
     * Returns the tag with the given key. Returns null if the tag does not exist
     * @param k the key
     * @return the tag with the given key. Returns null if the tag does not exist
     */
    @Transient
    public AtonTag getTag(String k) {
        return StringUtils.isBlank(k)
                ? null
                : Arrays.asList(tags).stream()
                .filter(t -> k.equals(t.getK()))
                .findFirst()
                .orElse(null);
    }
}
