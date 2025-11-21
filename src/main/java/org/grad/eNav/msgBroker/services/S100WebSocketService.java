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
import org.grad.eNav.msgBroker.models.*;
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Objects;

/**
 * The S100 Web-Socket Service Class
 * <p/>
 * This class implements a handler for the S100 messages coming into a Spring
 * Integration channel. It basically just publishes them to another channel,
 * which happens to be a web-socket implementation.
 *
 * @author Nikolaos Vastardis
 */
@Service
@Slf4j
public class S100WebSocketService implements MessageHandler {

    /**
     * The maximum payload for the web-socket
     */
    final static private int MAX_PAYLOAD = 60000;

    /**
     * The General Destination Prefix
     */
    @Value("${gla.rad.msg-broker.web-socket.prefix:topic}")
    String prefix;

    /**
     * The S-100 Publish Channel to listen the AtoN messages to.
     */
    @Autowired
    @Qualifier("s100PublishChannel")
    PublishSubscribeChannel s100PublishChannel;

    /**
     * Attach the web-socket as a simple messaging template
     */
    @Autowired
    SimpMessagingTemplate webSocket;

    /**
     * The service post-construct operations where the handler auto-registers
     * it-self to the aton publication channel. Once successful, it will then
     * monitor the channel for all inputs coming through the REST API.
     */
    @PostConstruct
    public void init() {
        log.info("AtoN Message Web Socket Service is booting up...");
        this.s100PublishChannel.subscribe(this);
    }

    /**
     * When shutting down the application we need to make sure that all
     * threads have been gracefully shutdown as well.
     */
    @PreDestroy
    public void destroy() {
        log.info("AtoN Message Web Socket Service is shutting down...");
        if(this.s100PublishChannel != null) {
            this.s100PublishChannel.destroy();
        }
    }

    /**
     * This is a simple handler for the incoming messages. This is a generic
     * handler for any type of Spring Integration messages, but it should really
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
        if(endpoint.compareTo(PublicationType.NAVIGATION_WARNING.getType()) == 0) {
            // Check that this seems ot be a valid message
            if(!(message.getPayload() instanceof String payload)) {
                log.warn("Web-Socket message handler received a Navigation Warning message with erroneous format.");
                return;
            }

            // Get the Aton Node payload
            S124Node s124Node = new S124Node(
                    (String) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_S124_ID.getHeader()),
                    (JsonNode) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_GEOM.getHeader()),
                    payload
            );

            // A simple debug message;
            log.debug(String.format("Received Navigation Warning with UID: %s.", s124Node.getMessageId()));

            // Now push the Navigation Warning node down the web-socket stream
            this.pushNode(this.webSocket, String.format("/%s/%s", prefix, endpoint), s124Node);
        }
        else if(endpoint.compareTo(PublicationType.ATON.getType()) == 0) {
            // Check that this seems ot be a valid message
            if(!(message.getPayload() instanceof String payload)) {
                log.warn("Web-Socket message handler received an AtoN message with erroneous format.");
                return;
            }

            // Get the Aton Node payload
            S125Node s125Node = new S125Node(
                    (String) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader()),
                    (JsonNode) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_GEOM.getHeader()),
                    payload
            );

            // A simple debug message;
            log.debug(String.format("Received AtoN Message with UID: %s.", s125Node.getDatasetUID()));

            // Now push the AtoN node down the web-socket stream
            this.pushNode(this.webSocket, String.format("/%s/%s", prefix, endpoint), s125Node);
        }
        else if(endpoint.compareTo(PublicationType.ADMIN_ATON.getType()) == 0) {
            // Check that this seems ot be a valid message
            if(!(message.getPayload() instanceof String payload)) {
                log.warn("Web-Socket message handler received an Admin AtoN message with erroneous format.");
                return;
            }

            // Get the Admin Aton Node payload
            S201Node s201Node = new S201Node(
                    (String) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_S201_ID.getHeader()),
                    (JsonNode) message.getHeaders().get(PubSubMsgHeaders.PUBSUB_GEOM.getHeader()),
                    payload
            );

            // A simple debug message;
            log.debug(String.format("Received Admin AtoN Message with UID: %s.", s201Node.getDatasetUID()));

            // Now push the Admin AtoN node down the web-socket stream
            this.pushNode(this.webSocket, String.format("/%s/%s", prefix, endpoint), s201Node);
        }
    }

    /**
     * Pushes a new/updated node into the Web-Socket messaging template.
     *
     * @param messagingTemplate     The web-socket messaging template
     * @param topic                 The topic of the web-socket
     * @param payload               The node payload to be pushed
     */
    private void pushNode(SimpMessagingTemplate messagingTemplate, String topic, Object payload) {
        messagingTemplate.convertAndSend(topic, payload);
    }

}
