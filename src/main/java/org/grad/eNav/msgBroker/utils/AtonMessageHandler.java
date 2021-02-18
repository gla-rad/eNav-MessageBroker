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
import org.geotools.data.DataUtilities;
import org.grad.eNav.msgBroker.models.AtonNode;
import org.grad.eNav.msgBroker.models.AtonTag;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * The RadarMessageHandler Class
 *
 * This class implements a handler for the AtoN messages coming into a Spring
 * Integration channel. It basically just published them to another channel,
 * which happens to be a web-socket implementation.
 *
 * @author Nikolaos Vastardis
 */
@Component
@Slf4j
public class AtonMessageHandler implements MessageHandler {

    /**
     * The General Destination Prefix
     */
    @Value("${gla.rad.radar-service.web-socket.prefix:topic}")
    private String prefix;

    /**
     * Attach the web-socket as a simple messaging template
     */
    @Autowired
    private SimpMessagingTemplate webSocket;

    /**
     * This is a simple handles for the incoming messages. This is a generic
     * handler for any type of Spring Integration messages but it should really
     * only be used for the ones containing RadarMessage payloads.
     *
     * @param message               The message to be handled
     * @throws MessagingException   The Messaging exceptions that might occur
     */
    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        // Check that this seems ot be a valid message
        if(!(message.getPayload() instanceof SimpleFeature)) {
            log.warn("Radar message handler received a message with erroneous format.");
            return;
        }

        // Get the header and payload of the radar message
        String endpoint = Objects.toString(message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
        SimpleFeature feature = SimpleFeature.class.cast(message.getPayload());

        // A simple debug message;
        log.info(DataUtilities.encodeFeature((feature)));

        // Create the AtoN Node message
        AtonNode atonNode = new AtonNode();
        atonNode.setUser((String)feature.getAttribute("user"));
        atonNode.setTimestamp((Date)feature.getAttribute("timestamp"));
        atonNode.setLon(((Point)feature.getAttribute("geom")).getX());
        atonNode.setLat(((Point)feature.getAttribute("geom")).getY());
        List<AtonTag> tags = new ArrayList<>();
        tags.add(new AtonTag("atonUID", (String)feature.getAttribute("atonUID")));
        atonNode.setTags(tags.toArray(new AtonTag[]{}));

        // And publish it at the appropriate endpoint
        this.webSocket.convertAndSend(String.format("/%s/%s", prefix, endpoint), atonNode);
    }

}
