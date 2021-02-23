/*
 * Copyright 2021 GLA UK Research and Development Directive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.msgBroker.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotools.data.Query;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.factory.Hints;
import org.grad.eNav.msgBroker.exceptions.InternalServerErrorException;
import org.locationtech.geomesa.utils.interop.SimpleFeatureTypes;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The implementation of the AtoN data entrie transported through the Geomesa
 * data stores.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class GeomesaAton implements GeomesaData<AtonNode>{

    // Class Variables
    private SimpleFeatureType sft = null;
    private List<SimpleFeature> features = null;
    private List<Query> queries = null;
    private Filter subsetFilter = null;

    /**
     * Defines the Geomesa Data Type - AtoN.
     *
     * @return      The Geomesa Data Type
     */
    @Override
    public String getTypeName() {
        return "AtoN";
    }

    /**
     * Constructs the Geomesa Feature type. This is pretty similar to a database
     * schema definition. We need to specify the attributes (columns) and their
     * types. For AtoN nodes, we also have a generic list of tags. The easiest
     * way of hadling this is to encode it as a JSON array.
     *
     * @return      The AtoN node simple feature type
     */
    @Override
    public SimpleFeatureType getSimpleFeatureType() {
        if (sft == null) {
            // list the attributes that constitute the feature type
            // this is a reduced set of the attributes from GDELT 2.0
            StringBuilder attributes = new StringBuilder();

            attributes.append("atonId:Integer,");
            attributes.append("atonUID:String,");
            attributes.append("uid:Integer,");
            attributes.append("user:String,");
            attributes.append("visible:Boolean,");
            attributes.append("changeset:Integer,");
            attributes.append("version:Integer,");
            attributes.append("timestamp:Date,");
            attributes.append("*geom:Point:srid=4326,"); // the "*" denotes the default geometry (used for indexing)
            attributes.append("tags:String");

            // create the simple-feature type - use the GeoMesa 'SimpleFeatureTypes' class for best compatibility
            // may also use geotools DataUtilities or SimpleFeatureTypeBuilder, but some features may not work
            sft = SimpleFeatureTypes.createType(getTypeName(), attributes.toString());

            // use the user-data (hints) to specify which date field to use for primary indexing
            // if not specified, the first date attribute (if any) will be used
            // could also use ':default=true' in the attribute specification string
            sft.getUserData().put(SimpleFeatureTypes.DEFAULT_DATE_KEY, "atonUID");
        }
        return sft;
    }

    /**
     * A helper function that constructs a list of simple features based on the
     * provided list of input objects i.e. in this case AtoN nodes. It must
     * follow the same construct as the definition provided in the simple
     * feature type.
     *
     * @param atons     The list of AtoNs to generate the simple features
     * @return The simple features based on the provided object list
     */
    @Override
    public List<SimpleFeature> getFeatureData(List<AtonNode> atons) {
        if (features == null) {
            List<SimpleFeature> features = new ArrayList<>();

            // Use a geotools SimpleFeatureBuilder to create our features
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(getSimpleFeatureType());

            for(AtonNode aton: atons) {
                builder.set("atonId", aton.getId());
                builder.set("atonUID", aton.getAtonUid());
                builder.set("uid", aton.getUid());
                builder.set("user", aton.getUser());
                builder.set("visible", aton.getVisible());
                builder.set("changeset", aton.getChangeset());
                builder.set("version", aton.getVersion());
                builder.set("visible", aton.getVisible());
                builder.set("timestamp", aton.getTimestamp());
                builder.set("geom", "POINT (" + aton.getLon() + " " + aton.getLat() + ")");

                // Now map the tags as JSON
                try {
                    builder.set("tags", new ObjectMapper().writeValueAsString(aton.getTags()));
                } catch (JsonProcessingException e) {
                    throw new InternalServerErrorException(e.getMessage());
                }

                // be sure to tell GeoTools explicitly that we want to use the ID we provided
                builder.featureUserData(Hints.USE_PROVIDED_FID, Boolean.TRUE);

                // build the feature - this also resets the feature builder for the next entry
                // use the AtoN UID as the feature ID
                features.add(builder.buildFeature(aton.getAtonUid()));
            }
            this.features = Collections.unmodifiableList(features);
        }
        return features;
    }

    /**
     * A helper function that reverses the operation of the getFeatureData().
     * It will reconstruct the list of AtoNs form the provided list of simple
     * features based on the construct defined in the simple feature type.
     *
     * @param features  The list of simple features to be used
     * @return The list of objects reconstructed
     */
    public List<AtonNode> retrieveData(List<SimpleFeature> features) {
        // A sanity check
        if(features == null) {
            return Collections.emptyList();
        }

        // Otherwise map all the provided features
        return features.stream()
                .map(feature -> {
                    // Create the AtoN Node message
                    AtonNode atonNode = new AtonNode();
                    atonNode.setId((Integer)feature.getAttribute("atonId"));
                    atonNode.setUid((Integer)feature.getAttribute("uid"));
                    atonNode.setUser((String)feature.getAttribute("user"));
                    atonNode.setVisible((Boolean)feature.getAttribute("visible"));
                    atonNode.setChangeset((Integer)feature.getAttribute("changeset"));
                    atonNode.setVersion((Integer)feature.getAttribute("version"));
                    atonNode.setTimestamp((Date)feature.getAttribute("timestamp"));
                    atonNode.setLon(((Point)feature.getAttribute("geom")).getX());
                    atonNode.setLat(((Point)feature.getAttribute("geom")).getY());
                    List<AtonTag> tags = new ArrayList<>();
                    try {
                        tags.addAll(new ObjectMapper().readValue((String)feature.getAttribute("tags"), new TypeReference<List<AtonTag>>(){}));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    atonNode.setTags(tags.toArray(new AtonTag[]{}));
                    return atonNode;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of queries through which data can be filtered. Currenlty
     * we want to show all data.
     *
     * @return The list of queries to filter the data for
     */
    @Override
    public List<Query> getFeatureQueries() {
        if (queries == null) {
            List<Query> queries = new ArrayList<>();
            // this data set is meant to show streaming updates over time, so just return all features
            queries.add(new Query(getTypeName(), Filter.INCLUDE));
            this.queries = Collections.unmodifiableList(queries);
        }
        return queries;
    }

    /**
     * A subsequent filter to further refine the feature search.
     *
     * @return The subsequent filter to further refine the search
     */
    @Override
    public Filter getSubsetFilter() {
        return Filter.INCLUDE;
    }
}
