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

package org.grad.eNav.msgBroker.controllers;

import lombok.extern.slf4j.Slf4j;
import org.grad.eNav.msgBroker.models.PubSubMsgHeaders;
import org.grad.eNav.msgBroker.models.PublicationType;
import org.grad.eNav.msgBroker.utils.GeoJSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * The S-100 Publish Controller Class
 * <p/>
 * This is the home controller that allows other microservices to publish new or
 * edited S-100 messages (such as AtoNs) into the message broker for everybody
 * else to be informed.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/publish")
@Slf4j
public class S100PublishController {

    /**
     * The S-100 Publish Channel to publish the incoming S-100 messages to.
     */
    @Autowired
    @Qualifier("s100PublishChannel")
    PublishSubscribeChannel s100PublishChannel;

    /**
     * The S-100 Delete Channel to publish the incoming S-100 message deletions to.
     */
    @Autowired
    @Qualifier("s100DeleteChannel")
    PublishSubscribeChannel s100DeleteChannel;

    /**
     * Receives an S-124 Navigation Warning as a REST POST request and pushes it
     * as a publication to the Geomesa Data Store.
     *
     * @param nwUID     The S124 Navigation Warning UID to be published
     * @param geometry  The geometry to publish the S-124 for
     * @param content   The S-124 XML message to be published
     * @return The received Navigation Warning along with the HTTP response
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(
            value = "/s124/{nwUID}",
            consumes = {"application/gml+xml;charset=UTF-8"},
            produces = {"application/gml+xml;charset=UTF-8"})
    public ResponseEntity<String> publishS124(@PathVariable("nwUID") String nwUID,
                                              @RequestParam List<Double> geometry,
                                              @RequestBody String content) {
        // Publish the AtoN message
        try {
            Optional.of(content)
                    .map(MessageBuilder::withPayload)
                    .map(builder -> {
                        builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.NAVIGATION_WARNING.getType());
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_S124_ID.getHeader(), nwUID);
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), GeoJSONUtils.createGeoJSONPolygon(geometry));
                        return builder;
                    })
                    .map(MessageBuilder::build)
                    .map(this.s100PublishChannel::send);
        } catch (NullPointerException | IllegalArgumentException ex) {
            log.error(ex.getMessage());
            return ResponseEntity.badRequest()
                    .build();
        }

        // If the publication was successful, return OK
        return ResponseEntity.ok(content);
    }

    /**
     * Receives an S-124 Navigation Warning UID as a REST DELETE request and
     * forwards it as a publication deletion from the Geomesa Data Store.
     *
     * @param nwUID   The S124 AtoN Navigation Warning to be deleted
     * @return The deleted Navigation Warning along with the HTTP response
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping(value = "/s124/{nwUID}")
    public ResponseEntity<String> deleteS124(@PathVariable("nwUID") String nwUID) {
        // Publish the AtoN deletion message
        try {
            Optional.of("Deletion")
                    .map(MessageBuilder::withPayload)
                    .map(builder -> {
                        builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.NAVIGATION_WARNING_DEL.getType());
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_S124_ID.getHeader(), nwUID);
                        return builder;
                    })
                    .map(MessageBuilder::build)
                    .map(this.s100DeleteChannel::send);
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.badRequest()
                    .build();
        }

        // If the publication was successful, return OK
        return ResponseEntity.ok().build();
    }

    /**
     * Receives an S-125 message  as a REST POST request and pushes it as a
     * publication to the Geomesa Data Store.
     *
     * @param atonUID   The S-125 AtoN UID to be published
     * @param geometry  The geometry to publish the S-125 for
     * @param content   The S-125 XML message to be published
     * @return The received AtoN along with the HTTP response
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(
            value = "/s125/{atonUID}",
            consumes = {"application/gml+xml;charset=UTF-8"},
            produces = {"application/gml+xml;charset=UTF-8"})
    public ResponseEntity<String> publishS125(@PathVariable("atonUID") String atonUID,
                                              @RequestParam List<Double> geometry,
                                              @RequestBody String content) {
        // Publish the AtoN message
        try {
            Optional.of(content)
                    .map(MessageBuilder::withPayload)
                    .map(builder -> {
                        builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON.getType());
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader(), atonUID);
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), GeoJSONUtils.createGeoJSONPolygon(geometry));
                        return builder;
                    })
                    .map(MessageBuilder::build)
                    .map(this.s100PublishChannel::send);
        } catch (NullPointerException | IllegalArgumentException ex) {
            log.error(ex.getMessage());
            return ResponseEntity.badRequest()
                    .build();
        }

        // If the publication was successful, return OK
        return ResponseEntity.ok(content);
    }

    /**
     * Receives an S-125 AtoN UID as a REST DELETE request and forwards it as a
     * publication deletion from the Geomesa Data Store.
     *
     * @param atonUID   The S-125 AtoN UID to be deleted
     * @return The deleted AtoN along with the HTTP response
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping(value = "/s125/{atonUID}")
    public ResponseEntity<String> deleteS125(@PathVariable("atonUID") String atonUID) {
        // Publish the AtoN deletion message
        try {
            Optional.of("Deletion")
                    .map(MessageBuilder::withPayload)
                    .map(builder -> {
                        builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON_DEL.getType());
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader(), atonUID);
                        return builder;
                    })
                    .map(MessageBuilder::build)
                    .map(this.s100DeleteChannel::send);
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.badRequest()
                    .build();
        }

        // If the publication was successful, return OK
        return ResponseEntity.ok().build();
    }

    /**
     * Receives an S-201 message  as a REST POST request and pushes it as a
     * publication to the Geomesa Data Store.
     *
     * @param atonUID   The S-201 AtoN UID to be published
     * @param geometry  The geometry to publish the S125 for
     * @param content   The S-201 XML message to be published
     * @return The received AtoN along with the HTTP response
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(
            value = "/s201/{atonUID}",
            consumes = {"application/gml+xml;charset=UTF-8"},
            produces = {"application/gml+xml;charset=UTF-8"})
    public ResponseEntity<String> publishS201(@PathVariable("atonUID") String atonUID,
                                              @RequestParam List<Double> geometry,
                                              @RequestBody String content) {
        // Publish the AtoN message
        try {
            Optional.of(content)
                    .map(MessageBuilder::withPayload)
                    .map(builder -> {
                        builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ADMIN_ATON.getType());
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_S201_ID.getHeader(), atonUID);
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_GEOM.getHeader(), GeoJSONUtils.createGeoJSONPolygon(geometry));
                        return builder;
                    })
                    .map(MessageBuilder::build)
                    .map(this.s100PublishChannel::send);
        } catch (NullPointerException | IllegalArgumentException ex) {
            log.error(ex.getMessage());
            return ResponseEntity.badRequest()
                    .build();
        }

        // If the publication was successful, return OK
        return ResponseEntity.ok(content);
    }

    /**
     * Receives an S-201 AtoN UID as a REST DELETE request and forwards it as a
     * publication deletion from the Geomesa Data Store.
     *
     * @param atonUID   The S-201 AtoN UID to be deleted
     * @return The deleted AtoN along with the HTTP response
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping(value = "/s201/{atonUID}")
    public ResponseEntity<String> deleteS201(@PathVariable("atonUID") String atonUID) {
        // Publish the AtoN deletion message
        try {
            Optional.of("Deletion")
                    .map(MessageBuilder::withPayload)
                    .map(builder -> {
                        builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ADMIN_ATON_DEL.getType());
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_S201_ID.getHeader(), atonUID);
                        return builder;
                    })
                    .map(MessageBuilder::build)
                    .map(this.s100DeleteChannel::send);
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.badRequest()
                    .build();
        }

        // If the publication was successful, return OK
        return ResponseEntity.ok().build();
    }

}
