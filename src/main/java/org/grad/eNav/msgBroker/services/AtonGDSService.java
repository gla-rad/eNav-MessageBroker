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

package org.grad.eNav.msgBroker.services;

import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.grad.eNav.msgBroker.config.AtonListenerProperties;
import org.grad.eNav.msgBroker.exceptions.InternalServerErrorException;
import org.grad.eNav.msgBroker.exceptions.ValidationException;
import org.grad.eNav.msgBroker.models.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;

/**
 * The AtoN Geomesa Data Store Service.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AtonGDSService implements MessageHandler  {

    /**
     * The Kafka Brokers addresses.
     */
    @Value("${kafka.brokers:localhost:9092}" )
    private String kafkaBrokers;

    /**
     * The Kafka Zookeepers addresses.
     */
    @Value("${kafka.zookeepers:localhost:2181}" )
    private String kafkaZookeepers;

    /**
     * The Number of Kafka Consumers.
     */
    @Value("${kafka.consumer.count:1}" )
    private Integer noKafkaConsumers;

    /**
     * The Application Context.
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The AtoN Listener Properties.
     */
    @Autowired
    private AtonListenerProperties atonListenerProperties;

    /**
     * The AtoN Publish Channel to listen the AtoN messages to.
     */
    @Autowired
    @Qualifier("atonPublishChannel")
    private PublishSubscribeChannel atonPublishChannel;

    /**
     * The Geomesa Data Store.
     */
    @Autowired
    @Qualifier("gsDataStore")
    DataStore producer;

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
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        // Register a new listeners to the data channels
        atonPublishChannel.subscribe(this);
    }

    /**
     * When shutting down the application we need to make sure that all
     * threads have been gracefully shutdown as well.
     */
    @PreDestroy
    public void destroy() {
        log.info("Geomesa Data Store Service is shutting down...");
        this.producer.dispose();
        if(this.atonPublishChannel != null) {
            this.atonPublishChannel.destroy();
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
                    String.class.cast(message.getHeaders().get(PubSubCustomHeaders.PUBSUB_S125_ID)),
                    Double[].class.cast(message.getHeaders().get(PubSubCustomHeaders.PUBSUB_BBOX)),
                    String.class.cast(message.getPayload())
            );

            // Now push the aton node down the Geomesa Data Store
            this.pushAton(s125Node);
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
    private void createSchema(DataStore datastore, SimpleFeatureType sft) throws IOException {
        log.info("Creating schema: " + DataUtilities.encodeType(sft));
        // we only need to do the once - however, calling it repeatedly is a no-op
        datastore.createSchema(sft);
        log.info("Schema created");
    }

    /**
     * Pushes a new/updated AtoN node into the Geomesa Data Store. Currently
     * this only supports the Kafka Message Streams.
     *
     * @param s125          The S125 node to be pushed into the datastore
     */
    private void pushAton(S125Node s125) {
        // We need a valid producer to push the AtoN to
        if(this.producer == null) {
            throw new ValidationException("No valid Geomesa Data Store producer detected.");
        }

        // We need a valid AtoN so that is it published
        if(s125 == null) {
            throw new ValidationException("A valid AtoN is required for the publication.");
        }

        // Translate the AtoNs to the Geomesa simple features
        GeomesaS125 gmAton = new GeomesaS125();
        try {
            this.writeFeatures(this.producer, gmAton.getSimpleFeatureType(), gmAton.getFeatureData(Collections.singletonList(s125)));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    /**
     * A generic function that writes the provided data into the datastore
     * schema, based on the provided simple feature type. The list of features
     * are generic points of interest that will be send to the data store.
     *
     * @param datastore     The datastore to write the feature into
     * @param sft           The simple feature type
     * @param features      The list of features to be written to the data store
     * @throws IOException IO Exception thrown while writing into the data store
     */
    private void writeFeatures(DataStore datastore, SimpleFeatureType sft, List<SimpleFeature> features) throws IOException {
        SimpleFeatureStore producerFS = (SimpleFeatureStore) datastore.getFeatureSource(sft.getTypeName());
        producerFS.addFeatures(new ListFeatureCollection(sft, features));
    }

}
