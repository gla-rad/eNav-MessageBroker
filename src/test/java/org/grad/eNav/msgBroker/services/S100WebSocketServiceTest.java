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
import org.apache.commons.io.IOUtils;
import org.grad.eNav.msgBroker.models.*;
import org.grad.eNav.msgBroker.utils.GeoJSONUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S100WebSocketServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    S100WebSocketService s100WebSocketService;

    /**
     * The S-100 Publish Subscribe Channel mock.
     */
    @Mock
    PublishSubscribeChannel s100PublishChannel;

    /**
     * The S-100 Delete Channel to listen to the S-100 message deletions.
     */
    @Mock
    PublishSubscribeChannel s100DeleteChannel;

    /**
     * The Web Socket mock.
     */
    @Mock
    SimpMessagingTemplate webSocket;

    // Test Variables
    private String s124Xml;
    private String s125Xml;
    private String s201Xml;
    private S124Node s124Node;
    private S125Node s125Node;
    private S201Node s201Node;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws IOException {
        // Read a valid S-124 content to generate the pub-sub message for.
        InputStream s124In = new ClassPathResource("s124-msg.xml").getInputStream();
        this.s124Xml = IOUtils.toString(s124In, StandardCharsets.UTF_8);
        // Read a valid S-125 content to generate the pub-sub message for.
        InputStream s125In = new ClassPathResource("s125-msg.xml").getInputStream();
        this.s125Xml = IOUtils.toString(s125In, StandardCharsets.UTF_8);
        // Read a valid S-201 content to generate the pub-sub message for.
        InputStream s201In = new ClassPathResource("s201-msg.xml").getInputStream();
        this.s201Xml = IOUtils.toString(s201In, StandardCharsets.UTF_8);

        // Also create a GeoJSON point geometry for our S125 message
        JsonNode point = GeoJSONUtils.createGeoJSON(53.61, 1.594);

        // Now create the S125 node object
        this.s124Node = new S124Node("NW-001-01", point, this.s124Xml);
        this.s125Node = new S125Node("test_aton", point, this.s125Xml);
        this.s201Node = new S201Node("test_admin_aton", point, this.s201Xml);


        // Also set the web-socket service topic prefix
        this.s100WebSocketService.prefix = "topic";
    }

    /**
     * Test that the S125 web-socket service gets initialised correctly,
     * and it subscribes to the AtoN publish subscribe channel.
     */
    @Test
    void testInit() {
        // Perform the service call
        this.s100WebSocketService.init();

        verify(this.s100PublishChannel, times(1)).subscribe(this.s100WebSocketService);
        verify(this.s100DeleteChannel, times(1)).subscribe(this.s100WebSocketService);
    }

    /**
     * Test that the S125 web-socket service gets destroyed correctly,
     * and it un-subscribes from the AtoN publish subscribe channel.
     */
    @Test
    void testDestroy() {
        // Perform the service call
        this.s100WebSocketService.destroy();

        verify(this.s100PublishChannel, times(1)).destroy();
        verify(this.s100DeleteChannel, times(1)).destroy();
    }

    /**
     * Test that the Web-Socket controlling service can process correctly the
     * Navigation Warning messages published in the S-100 publish-subscribe
     * channel.
     */
    @Test
    void testHandleNavigationWarningMessage() {
        // Create a message to be handled
        Message<?> message = Optional.of(this.s124Xml).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.NAVIGATION_WARNING.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S124_ID.getHeader(), this.s124Node.getMessageId()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), this.s124Node.getGeometryAsJson()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service call
        this.s100WebSocketService.handleMessage(message);

        // Verify that we send a packet to the VDES port and get that packet
        ArgumentCaptor<String> topicArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<S124Node> payLoadArgument = ArgumentCaptor.forClass(S124Node.class);
        verify(this.webSocket, times(1)).convertAndSend(topicArgument.capture(), payLoadArgument.capture());

        // Verify the packet
        assertEquals("/topic/" + PublicationType.NAVIGATION_WARNING.getType(), topicArgument.getValue());
        assertEquals(this.s124Node, payLoadArgument.getValue());
    }

    /**
     * Test that the Web-Socket controlling service can process correctly the
     * AtoN messages published in the S-100 publish-subscribe channel.
     */
    @Test
    void testHandleAtoNMessage() {
        // Create a message to be handled
        Message<?> message = Optional.of(this.s125Xml).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader(), this.s125Node.getDatasetUID()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), this.s125Node.getGeometryAsJson()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service call
        this.s100WebSocketService.handleMessage(message);

        // Verify that we send a packet to the VDES port and get that packet
        ArgumentCaptor<String> topicArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<S125Node> payLoadArgument = ArgumentCaptor.forClass(S125Node.class);
        verify(this.webSocket, times(1)).convertAndSend(topicArgument.capture(), payLoadArgument.capture());

        // Verify the packet
        assertEquals("/topic/" + PublicationType.ATON.getType(), topicArgument.getValue());
        assertEquals(this.s125Node, payLoadArgument.getValue());
    }

    /**
     * Test that the Web-Socket controlling service can process correctly the
     * Admin AtoN messages published in the S-100 publish-subscribe channel.
     */
    @Test
    void testHandleAdminAtoNMessage() {
        // Create a message to be handled
        Message<?> message = Optional.of(this.s201Xml).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ADMIN_ATON.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S201_ID.getHeader(), this.s201Node.getDatasetUID()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), this.s201Node.getGeometryAsJson()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service call
        this.s100WebSocketService.handleMessage(message);

        // Verify that we send a packet to the VDES port and get that packet
        ArgumentCaptor<String> topicArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<S201Node> payLoadArgument = ArgumentCaptor.forClass(S201Node.class);
        verify(this.webSocket, times(1)).convertAndSend(topicArgument.capture(), payLoadArgument.capture());

        // Verify the packet
        assertEquals("/topic/" + PublicationType.ADMIN_ATON.getType(), topicArgument.getValue());
        assertEquals(this.s201Node, payLoadArgument.getValue());
    }

    /**
     * Test that we can only send appropriate messages down to the web-socket.
     */
    @Test
    void testHandleMessageWrongPayload() {
        // Change the message content type to something else
        Message<?> message1 = Optional.of(Collections.singleton("this is just not a string")).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.NAVIGATION_WARNING.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S124_ID.getHeader(), this.s124Node.getMessageId()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), this.s124Node.getGeometryAsJson()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Another message for S-125
        Message<?> message2 = Optional.of(Collections.singleton("this is just not a string")).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader(), this.s125Node.getDatasetUID()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), this.s125Node.getGeometryAsJson()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Another message for S-201
        Message<?> message3 = Optional.of(Collections.singleton("this is just not a string")).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ADMIN_ATON.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S201_ID.getHeader(), this.s201Node.getDatasetUID()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), this.s201Node.getGeometryAsJson()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service calls
        this.s100WebSocketService.handleMessage(message1);
        this.s100WebSocketService.handleMessage(message2);
        this.s100WebSocketService.handleMessage(message3);

        // Verify that we didn't send any packets to the VDES port
        verify(this.webSocket, never()).convertAndSend(any(String.class), any(Object.class));
    }

}