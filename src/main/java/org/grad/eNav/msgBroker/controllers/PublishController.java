package org.grad.eNav.msgBroker.controllers;

import org.grad.eNav.msgBroker.feign.NiordClient;
import org.grad.eNav.msgBroker.models.PubSubCustomHeaders;
import org.grad.eNav.msgBroker.models.PublicationType;
import org.grad.eNav.msgBroker.services.AtonGDSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.bind.annotation.*;

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
    private AtonGDSService atonGDSService;

    /**
     * Receives an AtoN as a REST POST request and pushes it as a publication to
     * the Geomesa Data Store.
     *
     * @param x125      The S125 message to be published
     * @return The receive AtoN along with the HTTP response
     */
    @PostMapping(
            value = "/atons",
            consumes = {MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> publishAton(@RequestParam String atonUID,
                                              @RequestParam double[] bbox,
                                              @RequestBody String x125) {
        // Publish the message
        Optional.ofNullable(x125)
                .map(MessageBuilder::withPayload)
                .map(builder -> {
                    builder.setHeader(MessageHeaders.CONTENT_TYPE, PublicationType.ATON.getType());
                    builder.setHeader(PubSubCustomHeaders.PUBSUB_S125_ID, atonUID);
                    builder.setHeader(PubSubCustomHeaders.PUBSUB_BBOX, bbox);
                    return builder;
                })
                .map(MessageBuilder::build)
                .map(this.atonPublishChannel::send);
        // If the publication was successful, return OK
        return new ResponseEntity<String>(x125, HttpStatus.OK);
    }
}
