package org.grad.eNav.msgBroker.controllers;

import lombok.extern.slf4j.Slf4j;
import org.grad.eNav.msgBroker.feign.NiordClient;
import org.grad.eNav.msgBroker.models.PubSubCustomHeaders;
import org.grad.eNav.msgBroker.models.PublicationType;
import org.grad.eNav.msgBroker.services.S125GDSService;
import org.grad.eNav.msgBroker.utils.GeoJSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * The AtoN Controller.
 *
 * This is the home controller that allows other microservices to publish new or
 * edited information (such as AtoNs) into the message broker for everybody else
 * to be informed.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RequestMapping("/publish")
@RestController
@Slf4j
public class PublishController {

    /**
     * The Niord Client.
     */
    @Autowired
    private NiordClient niordClient;

    /**
     * The AtoN Data Channel to publish the incoming data to.
     */
    @Autowired
    @Qualifier("atonPublishChannel")
    private PublishSubscribeChannel atonPublishChannel;

    /**
     * The AtoN Geomesa Data Store Service.
     */
    @Autowired
    private S125GDSService s125GDSService;

    /**
     * Receives an AtoN as a REST POST request and pushes it as a publication to
     * the Geomesa Data Store.
     *
     * @param x125      The S125 message to be published
     * @return The receive AtoN along with the HTTP response
     */
    @PostMapping(
            value = "/atons/{atonUID}",
            consumes = {"application/gml+xml;charset=UTF-8"},
            produces = {"application/gml+xml;charset=UTF-8"})
    public ResponseEntity<String> publishAton(@PathVariable("atonUID") String atonUID,
                                              @RequestParam List<Double> bbox,
                                              @RequestBody String x125) {
        // Publish the message
        try {
            Optional.of(x125)
                    .map(MessageBuilder::withPayload)
                    .map(builder -> {
                        builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON.getType());
                        builder.setHeader(PubSubCustomHeaders.PUBSUB_S125_ID, atonUID);
                        builder.setHeader(PubSubCustomHeaders.PUBSUB_BBOX, GeoJSONUtils.createGeoJSONPoint(bbox.get(0), bbox.get(1)));
                        return builder;
                    })
                    .map(MessageBuilder::build)
                    .map(this.atonPublishChannel::send);
        } catch (NullPointerException ex) {
            log.error(ex.getMessage());
            return ResponseEntity.badRequest().build();
        }

        // If the publication was successful, return OK
        return new ResponseEntity<String>(x125, HttpStatus.OK);
    }
}
