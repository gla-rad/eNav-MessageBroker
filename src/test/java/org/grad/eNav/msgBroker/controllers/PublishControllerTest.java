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

import org.apache.commons.io.IOUtils;
import org.grad.eNav.msgBroker.services.S125GDSService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = PublishController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class PublishControllerTest {

    /**
     * The Mock MVC.
     */
    @Autowired
    MockMvc mockMvc;

    /**
     * The AtoN Publish Channel mock to publish the incoming AtoN messages to.
     */
    @MockBean
    @Qualifier("atonPublishChannel")
    PublishSubscribeChannel atonPublishChannel;

    /**
     * The AtoN Delete Channel mock to publish the incoming AtoN message deletions to.
     */
    @MockBean
    @Qualifier("atonDeleteChannel")
    PublishSubscribeChannel atonDeleteChannel;

    /**
     * The S125 Geomesa Datastore Service mock.
     */
    @MockBean
    S125GDSService s125GDSService;

    // Test Variables
    private String xml;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws IOException {
        // First read a valid S125 content to generate the publish-subscribe
        // message for.
        InputStream in = new ClassPathResource("s125-msg.xml").getInputStream();
        this.xml = IOUtils.toString(in, StandardCharsets.UTF_8.name());
    }

    /**
     * Test that we can publish an AtoN to the application's AtoN publish
     * subscribe channel by accessing the "publish/atons/{atonUID}" endpoint.
     */
    @Test
    void testPublishAton() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/publish/atons/{atonUID}", "aton.uk.test_aton")
                .contentType("application/gml+xml;charset=UTF-8")
                .param("bbox", "53.61, 1.594")
                .content(this.xml))
                .andExpect(status().isOk())
                .andReturn();

        // Make sure the S125 XML body is also returned for validation
        assertEquals(this.xml, mvcResult.getResponse().getContentAsString());
    }

    /**
     * Test that when trying to publish an AtoN to the application's AtoN
     * publish subscribe channel, if no valid S125 XML is provided, an HTTP
     * Bad Response (400) will be returned.
     */
    @Test
    void testPublishAtonBadRequest() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/publish/atons/{atonUID}", "aton.uk.test_aton")
                .contentType("application/gml+xml;charset=UTF-8")
                .param("bbox", "53.61, 1.594"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test that we can publish an AtoN UID to the application's AtoN delete
     * subscribe channel by accessing the "publish/atons/{atonUID}" endpoint.
     */
    @Test
    void testDeleteAton() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(delete("/publish/atons/{atonUID}", "aton.uk.test_aton"))
                .andExpect(status().isOk())
                .andReturn();
    }

}