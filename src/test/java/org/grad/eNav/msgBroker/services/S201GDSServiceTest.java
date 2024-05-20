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
import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.grad.eNav.msgBroker.exceptions.InternalServerErrorException;
import org.grad.eNav.msgBroker.models.*;
import org.grad.eNav.msgBroker.utils.GeoJSONUtils;
import org.grad.eNav.msgBroker.utils.GeometryJSONConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class S201GDSServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    S201GDSService s201GDSService;

    /**
     * The S-100 Publish Channel mock to listen to the S-100 messages.
     */
    @Mock
    @Qualifier("s100PublishChannel")
    PublishSubscribeChannel s100PublishChannel;

    /**
     * The S-100 Delete Channel mock to listen to the S-100 message deletions.
     */
    @Mock
    @Qualifier("s100DeleteChannel")
    PublishSubscribeChannel s100DeleteChannel;

    /**
     * The Geomesa Data Store mock.
     */
    @Mock
    DataStore producer;

    // Test Variables
    private String xml;
    private S201Node s201Node;
    private SimpleFeatureStore simpleFeatureStore;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws IOException {
        // First read a valid S-201 content to generate the publish-subscribe
        // message for.
        InputStream in = new ClassPathResource("s201-msg.xml").getInputStream();
        this.xml = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Also create a GeoJSON point geometry for our S-201 message
        JsonNode point = GeoJSONUtils.createGeoJSON(53.61, 1.594);

        // Now create the S-201 node object
        this.s201Node = new S201Node("test_aton", point, this.xml);

        // Also mock the Geomesa Simple Feature Store
        this.simpleFeatureStore = mock(SimpleFeatureStore.class);
        this.s201GDSService.featureStore = this.simpleFeatureStore;
    }

    /**
     * Test that the S-201 Geomesa Datastore service can initialise correctly
     * and subscribes to the AtoN publish subscribe channel.
     */
    @Test
    void testInit() {
        // Perform the service call
        this.s201GDSService.init();

        // Verify that the service subscribed to the AtoN publish subscribe channel
        verify(this.s100PublishChannel, times(1)).subscribe(this.s201GDSService);
        verify(this.s100DeleteChannel, times(1)).subscribe(this.s201GDSService);
    }

    /**
     * Test that the S-201 Geomesa Datastore service will not initialise if a
     * valid Geomesa Datastore does NOT exist.
     */
    @Test
    void testInitNoDatastore() {
        // Remove the datastore producer
        this.s201GDSService.producer = null;

        // Perform the service call
        this.s201GDSService.init();

        // Verify that the service did NOT subscribe to the AtoN publish subscribe channel
        verify(this.s100PublishChannel, never()).subscribe(this.s201GDSService);
        verify(this.s100DeleteChannel, never()).subscribe(this.s201GDSService);
    }

    /**
     * Test that when the S-201 Geomesa Datastore service shuts down, the
     * Geomesa datastore will be disposed and the Aton publish subscribe
     * channel subscription will be removed.
     */
    @Test
    void testDestroy() {
        // Perform the service call
        this.s201GDSService.destroy();

        // Verify that the service shuts down gracefully
        verify(this.producer, times(1)).dispose();
        verify(this.s100PublishChannel, times(1)).destroy();
        verify(this.s100DeleteChannel, times(1)).destroy();
    }

    /**
     * Test that the S-201 Geomesa Datastore service can process correctly the
     * AtoN messages published in the AtoN publish-subscribe channel.
     */
    @Test
    void testHandleMessageAton() {
        doNothing().when(this.s201GDSService).pushAton(any());

        // Create a message to be handled
        Message message = Optional.of(this.xml).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ADMIN_ATON.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S201_ID.getHeader(), this.s201Node.getAtonUID()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), GeometryJSONConverter.convertFromGeometry(this.s201Node.getGeometry())))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service call
        this.s201GDSService.handleMessage(message);

        // Verify that we send a packet to the VDES port and get that packet
        ArgumentCaptor<S201Node> s201NodeArgument = ArgumentCaptor.forClass(S201Node.class);
        verify(this.s201GDSService, times(1)).pushAton(s201NodeArgument.capture());

        // Verify the packet
        assertEquals(this.s201Node, s201NodeArgument.getValue());
    }

    /**
     * Test that we can only send S-201 messages down to the Geomesa Datastore.
     */
    @Test
    void testHandleMessageAtonWrongPayload() {
        // Create a message to be handled
        Message message = Optional.of(Collections.singleton("this is just not a string")).map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ADMIN_ATON.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S201_ID.getHeader(), this.s201Node.getAtonUID()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), this.s201Node.getGeometry()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service call
        this.s201GDSService.handleMessage(message);

        // Verify that we send a packet to the VDES port and get that packet
        verify(this.s201GDSService, never()).pushAton(any());
    }

    /**
     * Test that the S-201 Geomesa Datastore service can process correctly the
     * AtoN deletion messages published in the AtoN deletion publish-subscribe
     * channel.
     */
    @Test
    void testHandleMessageAtonDelete() {
        doNothing().when(this.s201GDSService).deleteAton(any());

        // Create a message to be handled
        Message message = Optional.of("Deletion").map(MessageBuilder::withPayload)
                .map(builder -> builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ADMIN_ATON_DEL.getType()))
                .map(builder -> builder.setHeader(PubSubMsgHeaders.PUBSUB_S201_ID.getHeader(), this.s201Node.getAtonUID()))
                .map(MessageBuilder::build)
                .orElse(null);

        // Perform the service call
        this.s201GDSService.handleMessage(message);

        // Verify that we send a packet to the VDES port and get that packet
        ArgumentCaptor<String> atonUidArgument = ArgumentCaptor.forClass(String.class);
        verify(this.s201GDSService, times(1)).deleteAton(atonUidArgument.capture());

        // Verify the packet
        assertEquals(this.s201Node.getAtonUID(), atonUidArgument.getValue());
    }

    /**
     * Test that when the Geomesa Datastore service pushed an AtoN to the Kafka
     * datastore, a GeomesaS201 message will be send to the Kafka message bus.
     */
    @Test
    void testPushAton() throws IOException {
        doNothing().when(this.s201GDSService).writeFeatures(any(), any(), any());

        // Create a GeomesaS201 message for testing
        GeomesaS201 geomesaData = new GeomesaS201();

        // Perform the service call
        this.s201GDSService.pushAton(this.s201Node);

        // Assert that the new feature created will be send down to the datastore
        verify(this.s201GDSService, times(1)).writeFeatures(
                eq(this.simpleFeatureStore),
                eq(geomesaData.getSimpleFeatureType()),
                anyList()
        );
    }

    /**
     * Test that when the Geomesa Datastore service pushed an AtoN to the Kafka
     * datastore, if an error is raised, an InternalServerErrorException will
     * be thrown.
     */
    @Test
    void testPushAtonError() throws IOException {
        doThrow(IOException.class).when(this.s201GDSService).writeFeatures(any(), any(), any());

        // Perform the service call
        assertThrows(InternalServerErrorException.class, () ->
                this.s201GDSService.pushAton(this.s201Node)
        );
    }

    /**
     * Test that when the Geomesa Datastore service deletes an AtoN from the
     * Kafka datastore, the datastore delete feature function will be called.
     */
    @Test
    void testDeleteAton() throws IOException, CQLException {
        doNothing().when(this.s201GDSService).deleteFeatures(any(), any());

        // Perform the service call
        this.s201GDSService.deleteAton(this.s201Node.getAtonUID());

        // Assert that the AtoN UID will be used to delete the matching features
        // from the datastore
        verify(this.s201GDSService, times(1)).deleteFeatures(this.simpleFeatureStore, ECQL.toFilter("id in ('" + this.s201Node.getAtonUID() + "')" ));
    }

    /**
     * Test that when the Geomesa Datastore service deletes an AtoN UID from the
     * Kafka datastore, if an error is raised, an InternalServerErrorException
     * will be thrown.
     */
    @Test
    void testDeleteAtonError() throws IOException, CQLException {
        doThrow(IOException.class).when(this.s201GDSService).deleteFeatures(any(), any());

        // Perform the service call
        assertThrows(InternalServerErrorException.class, () ->
                this.s201GDSService.deleteAton(this.s201Node.getAtonUID())
        );
    }

    /**
     * Test that the S-201 Geomesa Datastore service can write the incoming
     * messages as features to the respective datastore correctly.
     */
    @Test
    void testWriteFeatures() throws IOException {
        // Create a GeomesaS201 message for testing
        GeomesaS201 geomesaData = new GeomesaS201();

        // Perform the service class
        this.s201GDSService.writeFeatures(
                this.simpleFeatureStore,
                geomesaData.getSimpleFeatureType(),
                geomesaData.getFeatureData(Collections.singletonList(this.s201Node))
        );

        // Verify that the provided features were added to the datastore
        verify(this.simpleFeatureStore, times(1)).addFeatures(any());
    }

    /**
     * Test that the S-201 Geomesa Datastore service can delete published
     * messages as features from the respective datastore correctly.
     */
    @Test
    void testDeleteFeatures() throws IOException {
        // Perform the service class
        this.s201GDSService.deleteFeatures(
                this.simpleFeatureStore,
                mock(Filter.class)
        );

        // Verify that the provided features were added to the datastore
        verify(this.simpleFeatureStore, times(1)).removeFeatures(any());
    }

}