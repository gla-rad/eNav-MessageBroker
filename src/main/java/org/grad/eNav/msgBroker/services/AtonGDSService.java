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
import org.grad.eNav.msgBroker.config.AtonListenerProperties;
import org.grad.eNav.msgBroker.models.GeomesaAton;
import org.grad.eNav.msgBroker.utils.AtonMessageHandler;
import org.grad.eNav.msgBroker.utils.AtonGDSListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The AtoN Geomesa Data Store Service.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AtonGDSService {

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
     * The AtoN Data Channel to register the message handler into.
     */
    @Autowired
    @Qualifier("atonDataChannel")
    private PublishSubscribeChannel atonDataChannel;

    /**
     * The AtoN Message Handler
     */
    @Autowired
    private AtonMessageHandler atonMessageHandler;

    // Service Variables
    private DataStore consumer;
    private List<AtonGDSListener> dsListeners;

    /**
     * Once the service has been initialised, we can that start the execution
     * of the kafka data store listeners as a separate components that will run
     * on independent threads. All incoming messages with then be consumed by
     * the same handler, but handled based on the topic.
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
            this.consumer = this.createDataStore(params);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        // Create the AtoN Schema
        if(this.consumer == null) {
            log.error("Unable to connect to data store");
            return;
        }

        // Register a new listeners to the data channels
        atonDataChannel.subscribe(atonMessageHandler);

        // Get and initialise a the listener workers
        this.dsListeners = this.atonListenerProperties.getListeners()
                .stream()
                .map(listener -> {
                    AtonGDSListener dsListener = null;
                    try {
                        dsListener = this.applicationContext.getBean(AtonGDSListener.class);
                        dsListener.init(this.consumer,
                                        new GeomesaAton().getSimpleFeatureType(),
                                        listener.getAddress(),
                                        listener.getPort(),
                                        listener.getPolygon());
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                    return  dsListener;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * When shutting down the application we need to make sure that all
     * threads have been gracefully shutdown as well.
     */
    @PreDestroy
    public void destroy() {
        log.info("Geomesa Data Store is shutting down...");
        this.dsListeners.forEach(AtonGDSListener::destroy);
        this.atonDataChannel.destroy();
        this.consumer.dispose();
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
}
