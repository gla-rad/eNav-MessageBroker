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

package org.grad.eNav.msgBroker.utils;

import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureEvent;
import org.geotools.data.FeatureListener;
import org.locationtech.geomesa.kafka.utils.KafkaFeatureEvent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The AtoN Geomesa Data Store Listener Class
 *
 * This class defines the main operation of the AtoN listening operation
 * on the Geomesa Kafka Data Store.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Slf4j
@Transactional
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class AtonGDSListener {

    /**
     * The AtoN Data Channel to publish the incoming data to.
     */
    @Autowired
    @Qualifier("atonDataChannel")
    private PublishSubscribeChannel atonDataChannel;

    // Component Variables
    private DataStore consumer;
    private FeatureListener listener;
    private SimpleFeatureType sft;
    private String vdesAddress;
    private Integer vdesPort;
    private List<Double> listenerArea;

    /**
     * Once the listener has been initialised, it will create a consumer of
     * the data store provided and publish the incoming messages into the
     * AtoN data channel.
     *
     * @param consumer      The data store to consume the messages from
     */
    public void init(DataStore consumer,
                     SimpleFeatureType sft,
                     String vdesAddress,
                     Integer vdesPort,
                     List<Double> listenerArea) throws IOException {
        this.consumer = consumer;
        this.sft = sft;
        this.vdesAddress = vdesAddress;
        this.vdesPort = vdesPort;
        this.listenerArea = Optional.ofNullable(listenerArea).orElse(Collections.emptyList());
        this.listener = (this::listenToEvents);
        this.consumer.getFeatureSource(sft.getTypeName()).addFeatureListener(listener);

        // Log an information message
        log.info(String.format("Initialised AtoN listener for VDES at %s:%d for area: %s",
                this.vdesAddress,
                this.vdesPort,
                this.listenerArea.stream().map(String::valueOf).collect(Collectors.joining(","))));
    }

    /**
     * When shutting down the application we need to make sure that all
     * threads have been gracefully shutdown as well.
     */
    @PreDestroy
    public void destroy() {
        log.info("AtoN Data Listener is shutting down...");
        try {
            this.consumer.getFeatureSource(this.sft.getTypeName()).removeFeatureListener(this.listener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main data store listener operation where events are being handled.
     *
     * @param featureEvent      The feature event that took place
     */
    private void listenToEvents(FeatureEvent featureEvent) {
        log.info("Received FeatureEvent from schema '" + this.sft.getTypeName() + "' of type '" + featureEvent.getType() + "'");
        if(featureEvent.getType() == FeatureEvent.Type.CHANGED) {
            // We only listen to kafka events for processing
            if(featureEvent instanceof KafkaFeatureEvent.KafkaFeatureChanged) {
                SimpleFeature feature = ((KafkaFeatureEvent.KafkaFeatureChanged) featureEvent).feature();
                // Publish the message
                Message<SimpleFeature> channelMsg = MessageBuilder
                        .withPayload(feature)
                        .setHeader(MessageHeaders.CONTENT_TYPE, String.format("%s:%d", this.vdesAddress, this.vdesPort))
                        .build();
                this.atonDataChannel.send(channelMsg);
            }
        }
        else if(featureEvent.getType() == FeatureEvent.Type.REMOVED) {
            log.info("Received Delete for filter: " + featureEvent.getFilter());
        }
    }

}
