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
import org.grad.eNav.msgBroker.models.PubSubCustomHeaders;
import org.grad.eNav.msgBroker.models.PublicationType;
import org.grad.eNav.msgBroker.models.S125Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;

/**
 * The AtonWebSocketService Class
 *
 * This class implements a handler for the AtoN messages coming into a Spring
 * Integration channel. It basically just published them to another channel,
 * which happens to be a web-socket implementation.
 *
 * @author Nikolaos Vastardis
 */
@Service
@Slf4j
public class AtonWebSocketService implements MessageHandler {

    /**
     * The General Destination Prefix
     */
    @Value("${gla.rad.msg-broker.web-socket.prefix:topic}")
    private String prefix;

    /**
     * The AtoN Publish Channel to listen the AtoN messages to.
     */
    @Autowired
    @Qualifier("atonPublishChannel")
    private PublishSubscribeChannel atonPublishChannel;

    /**
     * Attach the web-socket as a simple messaging template
     */
    @Autowired
    private SimpMessagingTemplate webSocket;

    /**
     * The service post-construct operations where the handler auto-registers
     * it-self to the aton publication channel. Once successful, it will then
     * monitor the channel for all inputs coming through the REST API.
     */
    @PostConstruct
    public void init() {
        log.info("AtoN Message Web Socket Service is booting up...");
        this.atonPublishChannel.subscribe(this);
    }

    /**
     * When shutting down the application we need to make sure that all
     * threads have been gracefully shutdown as well.
     */
    @PreDestroy
    public void destroy() {
        log.info("AtoN Message Web Socket Service is shutting down...");
        if(this.atonPublishChannel != null) {
            this.atonPublishChannel.destroy();
        }
    }

    /**
     * This is a simple handler for the incoming messages. This is a generic
     * handler for any type of Spring Integration messages but it should really
     * only be used for the ones containing RadarMessage payloads.
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
            if(!(message.getPayload() instanceof String)) {
                log.warn("Web-Socket message handler received a message with erroneous format.");
                return;
            }

            // Get the Aton Node payload
            S125Node s125Node = new S125Node(
                    String.class.cast(message.getHeaders().get(PubSubCustomHeaders.PUBSUB_S125_ID)),
                    double[].class.cast(message.getHeaders().get(PubSubCustomHeaders.PUBSUB_BBOX)),
                    String.class.cast(message.getPayload())
            );

            // A simple debug message;
            log.debug(String.format("Received AtoN Message with UID: %s.", s125Node.getAtonUID()));

            // And publish it at the appropriate endpoint
            this.webSocket.convertAndSend(String.format("/%s/%s", prefix, endpoint), s125Node);
        }



    }

}
