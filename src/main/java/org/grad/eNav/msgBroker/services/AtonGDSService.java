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
import org.grad.eNav.msgBroker.models.AtonNode;
import org.grad.eNav.msgBroker.models.GeomesaAton;
import org.grad.eNav.msgBroker.models.PublicationType;
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
     * The Application Context
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The AtoN Listener Properties
     */
    @Autowired
    private AtonListenerProperties atonListenerProperties;

    /**
     * The AtoN Publish Channel to listen the AtoN messages to.
     */
    @Autowired
    @Qualifier("atonPublishChannel")
    private PublishSubscribeChannel atonPublishChannel;

    // Service Variables
    private DataStore producer;

    /**
     * Once the service has been initialised, it will connect to a Kafka Message
     * Streaming service through the Geomesa Data Store. It will also monitor
     * the provided Spring Integration channel for incoming AtoN messages which
     * will then be passed onto Kafka for other interested microservices.
     */
    @PostConstruct
    public void init() {
        log.info("Geomesa Data Store Service is booting up...");

        Map<String, String> params = new HashMap<>();
        params.put("kafka.brokers", kafkaBrokers);
        params.put("kafka.zookeepers", kafkaZookeepers);
        params.put("kafka.consumer.count", Objects.toString(noKafkaConsumers));

        // Create the producer
        try {
            this.producer = this.createDataStore(params);

            // Create the AtoN Schema
            if(this.producer != null) {
                GeomesaAton gmAton = new GeomesaAton();
                this.createSchema(this.producer, gmAton.getSimpleFeatureType());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        // Create the AtoN Schema
        if(this.producer == null) {
            log.error("Unable to connect to data store");
            return;
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
     * @param message               The message to be handled
     * @throws MessagingException   The Messaging exceptions that might occur
     */
    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        // Get the header and payload of the incoming message
        String endpoint = Objects.toString(message.getHeaders().get(MessageHeaders.CONTENT_TYPE));

        // Check that the message type is correct
        if(endpoint.compareTo(PublicationType.ATON.getType()) == 0) {
            // Check that this seems ot be a valid message
            if(!(message.getPayload() instanceof AtonNode)) {
                log.warn("Radar message handler received a message with erroneous format.");
                return;
            }

            // Get the Aton Node payload
            AtonNode atonNode = AtonNode.class.cast(message.getPayload());

            // Now push the aton node down the Geomesa Data Store
            this.pushAton(atonNode);
        }
    }

    /**
     * Creates the Geomesa Data Store from scratch. There is no problem if the
     * store already exists, this will do nothing.
     *
     * @param params        The parameters for the generating the datastore
     * @return The generated data store
     * @throws IOException IO Exception thrown while accessing the data store
     */
    private DataStore createDataStore(Map<String, String> params) throws IOException {
        log.info("Creating GeoMesa Data Store");
        DataStore producer = DataStoreFinder.getDataStore(params);
        if (producer == null) {
            throw new RuntimeException("Could not create data store with provided parameters");
        }
        log.info("GeoMesa Data Store created");
        return producer;
    }

    /**
     * Creates a new schema in the provided data store. The schema is pretty
     * much like the table in a database that will accept the row data.
     *
     * @param datastore the datastore to create the schema into
     * @param sft the simple feature type i.e. the schema description
     * @throws IOException
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
     * @param aton the AtoN node to be pushed into the datastore
     */
    private void pushAton(AtonNode aton) {
        // We need a valid producer to push the AtoN to
        if(this.producer == null) {
            throw new ValidationException("No valid Geomesa Data Store producer detected.");
        }

        // We need a valid AtoN so that is it published
        if(aton == null) {
            throw new ValidationException("A valid AtoN is required for the publication.");
        }

        // Translate the AtoNs to the Geomesa simple features
        GeomesaAton gmAton = new GeomesaAton();
        try {
            this.writeFeatures(this.producer, gmAton.getSimpleFeatureType(), gmAton.getFeatureData(Collections.singletonList(aton)));
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
     * @param datastore the datastore to write the feature into
     * @param sft the simple feature type
     * @param features the list of features to be writter to the data store
     * @throws IOException
     */
    private void writeFeatures(DataStore datastore, SimpleFeatureType sft, List<SimpleFeature> features) throws IOException {
        SimpleFeatureStore producerFS = (SimpleFeatureStore) datastore.getFeatureSource(sft.getTypeName());
        producerFS.addFeatures(new ListFeatureCollection(sft, features));
    }

}
