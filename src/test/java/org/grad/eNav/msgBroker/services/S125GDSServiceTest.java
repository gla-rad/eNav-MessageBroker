/*
 * Copyright (c) 2021 GLA UK Research and Development Directive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureStore;
import org.grad.eNav.msgBroker.config.AtonListenerProperties;
import org.grad.eNav.msgBroker.exceptions.InternalServerErrorException;
import org.grad.eNav.msgBroker.models.GeomesaS125;
import org.grad.eNav.msgBroker.models.PubSubMsgHeaders;
import org.grad.eNav.msgBroker.models.PublicationType;
import org.grad.eNav.msgBroker.models.S125Node;
import org.grad.eNav.msgBroker.utils.GeoJSONUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S125GDSServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    S125GDSService s125GDSService;

    /**
     * The Application Context mock.
     */
    @Mock
    ApplicationContext applicationContext;

    /**
     * The AtoN Listener Properties mock.
     */
    @Mock
    AtonListenerProperties atonListenerProperties;

    /**
     * The AtoN Publish Channel mock.
     */
    @Mock
    PublishSubscribeChannel atonPublishChannel;

    /**
     * The Geomesa Data Store mock.
     */
    @Mock
    DataStore producer;

    // Test Variables
    private String xml;
    private S125Node s125Node;
    private SimpleFeatureStore simpleFeatureStore;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws IOException {
        // First read a valid S125 content to generate the publish-subscribe
        // message for.
        InputStream in = new ClassPathResource("s125-msg.xml").getInputStream();
        this.xml = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Also create a GeoJSON point geometry for our S125 message
        JsonNode point = GeoJSONUtils.createGeoJSONPoint(53.61, 1.594);

        // Now create the S125 node object
        this.s125Node = new S125Node("test_aton", point, this.xml);

        // Also mock the Geomesa Simple Feature Store
        this.simpleFeatureStore = mock(SimpleFeatureStore.class);
        this.s125GDSService.featureStore = this.simpleFeatureStore;
    }

    /**
     * Test that the S125 Geomesa Datastore service can initialise correctly
     * and subscribes to the AtoN publish subscribe channel.
     */
    @Test
    void testInit() {
        // Perform the service call
        this.s125GDSService.init();

        // Verify that the service subscribed to the AtoN publish subscribe channel
        verify(this.atonPublishChannel, times(1)).subscribe(this.s125GDSService);
    }

    /**
     * Test that the S125 Geomesa Datastore service will not initialise if a
     * valid Geomesa Datastore does NOT exist.
     */
    @Test
    void testInitNoDatastore() {
        // Remove the datastore producer
        this.s125GDSService.producer = null;

        // Perform the service call
        this.s125GDSService.init();

        // Verify that the service did NOT subscribe to the AtoN publish subscribe channel
        verify(this.atonPublishChannel, never()).subscribe(this.s125GDSService);
    }

    /**
     * Test that when the S125 Geomesa Datastore service shuts down, the
     * Geomesa datastore will be disposed and the Aton publish subscribe
     * channel subscription will be removed.
     */
    @Test
    void testDestroy() {
        // Perform the service call
        this.s125GDSService.destroy();

        // Verify that the service shuts down gracefully
        verify(this.producer, times(1)).dispose();
        verify(this.atonPublishChannel, times(1)).destroy();
    }

    /**
     * Test that the S125 Geomesa Datastore service can process correctly the
     * AtoN messages published in the AtoN publish-subscribe channel.
     */
    @Test
    void testHandleMessage() {
        doNothing().when(this.s125GDSService).pushAton(any());

        // Create a message to be handled
        Message message = Optional.of(this.xml).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader(), this.s125Node.getAtonUID()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_BBOX.getHeader(), this.s125Node.getBbox()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service call
        this.s125GDSService.handleMessage(message);

        // Verify that we send a packet to the VDES port and get that packet
        ArgumentCaptor<S125Node> s125NodeArgument = ArgumentCaptor.forClass(S125Node.class);
        verify(this.s125GDSService, times(1)).pushAton(s125NodeArgument.capture());

        // Verify the packet
        assertEquals(this.s125Node, s125NodeArgument.getValue());
    }

    /**
     * Test that we can only send S125 messages down to the Geomesa Datastore.
     */
    @Test
    void testHandleMessageWrongPayload() {
        // Create a message to be handled
        Message message = Optional.of(Collections.singleton("this is just not a string")).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader(), this.s125Node.getAtonUID()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_BBOX.getHeader(), this.s125Node.getBbox()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service call
        this.s125GDSService.handleMessage(message);

        // Verify that we send a packet to the VDES port and get that packet
        verify(this.s125GDSService, never()).pushAton(any());
    }

    /**
     * Test that when the Geomesa Datastore service pushed an AtoN to the Kafka
     * datastore, a GeomesaS125 message will be send to the Kafka message bus.
     */
    @Test
    void testPushAton() throws IOException {
        doNothing().when(this.s125GDSService).writeFeatures(any(), any(), any());

        // Create a GeomesaS125 message for testing
        GeomesaS125 geomesaData = new GeomesaS125();

        // Perform the service call
        this.s125GDSService.pushAton(this.s125Node);

        // Assert that the new feature created will be send down to the datastore
        verify(this.s125GDSService, times(1)).writeFeatures(this.simpleFeatureStore, geomesaData.getSimpleFeatureType(), geomesaData.getFeatureData(Collections.singletonList(s125Node)));
    }

    /**
     * Test that when the Geomesa Datastore service pushed an AtoN to the Kafka
     * datastore, if an error is raised, an InternalServerErrorException will
     * be thrown.
     */
    @Test
    void testPushAtonError() throws IOException {
        doThrow(IOException.class).when(this.s125GDSService).writeFeatures(any(), any(), any());

        // Perform the service call
        assertThrows(InternalServerErrorException.class, () ->
                this.s125GDSService.pushAton(this.s125Node)
        );
    }

    /**
     * Test that the S125 Geomesa Datastore service can write the incoming
     * messages as features to the respective datastore correctly.
     */
    @Test
    void testWriteFeatures() throws IOException {
        // Create a GeomesaS125 message for testing
        GeomesaS125 geomesaData = new GeomesaS125();

        // Perform the service class
        this.s125GDSService.writeFeatures(
                this.simpleFeatureStore,
                geomesaData.getSimpleFeatureType(),
                geomesaData.getFeatureData(Collections.singletonList(this.s125Node))
        );

        // Verify that the provided features were added to the datastore
        verify(this.simpleFeatureStore, times(1)).addFeatures(any());
    }
}