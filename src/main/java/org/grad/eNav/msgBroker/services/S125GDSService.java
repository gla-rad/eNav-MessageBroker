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

package org.grad.eNav.msgBroker.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.grad.eNav.msgBroker.exceptions.InternalServerErrorException;
import org.grad.eNav.msgBroker.exceptions.ValidationException;
import org.grad.eNav.msgBroker.models.GeomesaS125;
import org.grad.eNav.msgBroker.models.PubSubMsgHeaders;
import org.grad.eNav.msgBroker.models.PublicationType;
import org.grad.eNav.msgBroker.models.S125Node;
import org.geotools.api.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The S125 Geomesa Data Store Service Class
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class S125GDSService implements MessageHandler  {

    /**
     * The S-100 Publish Channel to listen to the S-100 messages.
     */
    @Autowired
    @Qualifier("s100PublishChannel")
    PublishSubscribeChannel s100PublishChannel;

    /**
     * The S-100 Delete Channel to listen to the S-100 message deletions.
     */
    @Autowired
    @Qualifier("s100DeleteChannel")
    PublishSubscribeChannel s100DeleteChannel;

    /**
     * The Geomesa Data Store.
     */
    @Autowired
    @Qualifier("gsDataStore")
    DataStore producer;

    // Service Variables
    SimpleFeatureStore featureStore;

    /**
     * Once the service has been initialised, it will connect to a Kafka Message
     * Streaming service through the Geomesa Data Store. It will also monitor
     * the provided Spring Integration channel for incoming AtoN messages which
     * will then be passed onto Kafka for other interested microservices.
     */
    @PostConstruct
    public void init() {
        log.info("Geomesa Data Store Service is booting up...");

        // Create the producer
        if(this.producer == null) {
            log.error("Unable to connect to data store");
            return;
        }

        // Create the AtoN Schema
        try {
            this.createSchema(this.producer, new GeomesaS125().getSimpleFeatureType());
            this.featureStore = (SimpleFeatureStore) this.producer.getFeatureSource(new GeomesaS125().getTypeName());
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        // Register a new listeners to the data channels
        this.s100PublishChannel.subscribe(this);
        this.s100DeleteChannel.subscribe(this);
    }

    /**
     * When shutting down the application we need to make sure that all
     * threads have been gracefully shutdown as well.
     */
    @PreDestroy
    public void destroy() {
        log.info("Geomesa Data Store Service is shutting down...");
        this.producer.dispose();
        if(this.s100PublishChannel != null) {
            this.s100PublishChannel.destroy();
        }
        if(this.s100DeleteChannel != null) {
            this.s100DeleteChannel.destroy();
        }
    }

    /**
     * This is a simple handler for the incoming messages. This is a generic
     * handler for any type of Spring Integration messages but it should really
     * only be used for the ones containing AtoN node payloads.
     *
     * @param message       The message to be handled
     * @throws MessagingException The Messaging exceptions that might occur
     */
    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        // Get the header and payload of the incoming message
        String endpoint = Objects.toString(message.getHeaders().get(MessageHeaders.CONTENT_TYPE));

        // Check that the message type is correct
        if(endpoint.compareTo(PublicationType.ATON.getType()) == 0) {
            // Check that this seems ot be a valid message
            if(!(message.getPayload() instanceof String)) {
                log.warn("Message-Broker message handler received a message with erroneous format.");
                return;
            }

            // Get the Aton Node payload
            S125Node s125Node = new S125Node(
                    (String) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader()),
                    (JsonNode) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_GEOM.getHeader()),
                    (String) message.getPayload()
            );

            // Now push the aton node down the Geomesa Data Store
            this.pushAton(s125Node);
        } else if(endpoint.compareTo(PublicationType.ATON_DEL.getType()) == 0) {
            this.deleteAton((String) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader()));
        }
    }

    /**
     * Creates a new schema in the provided data store. The schema is pretty
     * much like the table in a database that will accept the row data.
     *
     * @param datastore     The datastore to create the schema into
     * @param sft           The simple feature type i.e. the schema description
     * @throws IOException IO Exception thrown while creating the data store
     */
    protected void createSchema(DataStore datastore, SimpleFeatureType sft) throws IOException {
        log.info("Creating schema: " + DataUtilities.encodeType(sft));
        // we only need to do the once - however, calling it repeatedly is a no-op
        datastore.createSchema(sft);
        log.info("Schema created");
    }

    /**
     * Pushes a new/updated AtoN node into the Geomesa Data Store. Currently,
     * this only supports the Kafka Message Streams.
     *
     * @param s125Node          The S-125 node to be pushed into the datastore
     */
    protected void pushAton(S125Node s125Node) {
        // We need a valid producer to push the AtoN to
        if(this.producer == null) {
            throw new ValidationException("No valid Geomesa Data Store producer detected.");
        }

        // Translate the AtoNs to the Geomesa simple features
        GeomesaS125 geomesaData = new GeomesaS125();
        try {
            this.writeFeatures(this.featureStore,
                    geomesaData.getSimpleFeatureType(),
                    geomesaData.getFeatureData(Collections.singletonList(s125Node)));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    /**
     * Pushes a new/updated AtoN node into the Geomesa Data Store. Currently,
     * this only supports the Kafka Message Streams. Also in the used Geomesa
     * implementation (3.3.0 in the time of writing) only ID filtering is
     * supporting for the feature removals, so geometry has to stay out for
     * the time being.
     *
     * @param atonUid           The AtoN UID to be deleted from the datastore
     */
    protected void deleteAton(String atonUid) {
        // We need a valid producer to push the AtoN to
        if(this.producer == null) {
            throw new ValidationException("No valid Geomesa Data Store producer detected.");
        }

        // Translate the AtoNs UID to the Geomesa ECQl filters
        try {
            this.deleteFeatures(this.featureStore,
                    ECQL.toFilter("id in ('" + atonUid + "')" ));
        } catch (CQLException | IOException e) {
            log.error(e.getMessage());
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    /**
     * A generic function that writes the provided data into the datastore
     * schema, based on the provided simple feature type. The list of features
     * are generic points of interest that will be sent to the data store.
     *
     * @param datastore     The datastore to write the feature into
     * @param sft           The simple feature type
     * @param features      The list of features to be written to the data store
     * @throws IOException IO Exception thrown while writing into the data store
     */
    protected void writeFeatures(SimpleFeatureStore datastore, SimpleFeatureType sft, List<SimpleFeature> features) throws IOException {
        datastore.addFeatures(new ListFeatureCollection(sft, features));
    }

    /**
     * A generic function that deletes the selected data entries from the
     * datastore schema, based on the provided filter.
     *
     * @param datastore     The datastore to write the feature into
     * @param filter        The filter to delete the data for
     * @throws IOException IO Exception thrown while writing into the data store
     */
    protected void deleteFeatures(SimpleFeatureStore datastore, Filter filter) throws IOException {
        datastore.removeFeatures(filter);
    }

}
