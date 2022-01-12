/*
 * Copyright (c) 2021 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.grad.eNav.msgBroker.services.S125GDSService;
import org.grad.eNav.msgBroker.utils.GeoJSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * The Publish Controller Class
 *
 * This is the home controller that allows other microservices to publish new or
 * edited information (such as AtoNs) into the message broker for everybody else
 * to be informed.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/publish")
@Slf4j
public class PublishController {

    /**
     * The AtoN Publish Channel to publish the incoming AtoN messages to.
     */
    @Autowired
    @Qualifier("atonPublishChannel")
    PublishSubscribeChannel atonPublishChannel;

    /**
     * The AtoN Delete Channel to publish the incoming AtoN message deletions to.
     */
    @Autowired
    @Qualifier("atonDeleteChannel")
    PublishSubscribeChannel atonDeleteChannel;

    /**
     * The AtoN Geomesa Data Store Service.
     */
    @Autowired
    S125GDSService s125GDSService;

    /**
     * Receives an AtoN as a REST POST request and pushes it as a publication to
     * the Geomesa Data Store.
     *
     * @param atonUID   The S125 AtoN UID to be published
     * @param bbox      The bounding box to publish the S125 for
     * @param s125xml   The S125 XML message to be published
     * @return The receive AtoN along with the HTTP response
     */
    @PostMapping(
            value = "/atons/{atonUID}",
            consumes = {"application/gml+xml;charset=UTF-8"},
            produces = {"application/gml+xml;charset=UTF-8"})
    public ResponseEntity<String> publishAton(@PathVariable("atonUID") String atonUID,
                                              @RequestParam List<Double> bbox,
                                              @RequestBody String s125xml) {
        // Publish the AtoN message
        try {
            Optional.of(s125xml)
                    .map(MessageBuilder::withPayload)
                    .map(builder -> {
                        builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON.getType());
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader(), atonUID);
                        builder.setHeader(PubSubMsgHeaders.PUBSUB_BBOX.getHeader(), GeoJSONUtils.createGeoJSONPoint(bbox.get(0), bbox.get(1)));
                        return builder;
                    })
                    .map(MessageBuilder::build)
                    .map(this.atonPublishChannel::send);
        } catch (NullPointerException ex) {
            log.error(ex.getMessage());
            return ResponseEntity.badRequest()
                    .build();
        }

        // If the publication was successful, return OK
        return ResponseEntity.ok(s125xml);
    }

    /**
     * Receives an AtoN UID as a REST DELETE request and forwards it as a
     * publication deletion from the Geomesa Data Store.
     *
     * @param atonUID   The S125 AtoN UID to be deleted
     * @return The receive AtoN along with the HTTP response
     */
    @DeleteMapping(value = "/atons/{atonUID}")
    public ResponseEntity<String> deleteAton(@PathVariable("atonUID") String atonUID) {
        // Publish the AtoN deletion message
        Optional.of("Deletion")
                .map(MessageBuilder::withPayload)
                .map(builder -> {
                    builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON_DEL.getType());
                    builder.setHeader(PubSubMsgHeaders.PUBSUB_S125_ID.getHeader(), atonUID);
                    return builder;
                })
                .map(MessageBuilder::build)
                .map(this.atonDeleteChannel::send);

        // If the publication was successful, return OK
        return ResponseEntity.ok().build();
    }

}
