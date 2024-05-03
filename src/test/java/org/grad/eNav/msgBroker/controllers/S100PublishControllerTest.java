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

import org.apache.commons.io.IOUtils;
import org.grad.eNav.msgBroker.TestingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = S100PublishController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
class S100PublishControllerTest {

    /**
     * The Mock MVC.
     */
    @Autowired
    MockMvc mockMvc;

    /**
     * The S-100 Publish Channel mock to publish the incoming S-100 messages to.
     */
    @MockBean
    @Qualifier("s100PublishChannel")
    PublishSubscribeChannel s100PublishChannel;

    /**
     * The S-100 Delete Channel mock to publish the incoming S-100 message deletions to.
     */
    @MockBean
    @Qualifier("s100DeleteChannel")
    PublishSubscribeChannel s100DeleteChannel;

    // Test Variables
    private String s124Xml;
    private String s125Xml;
    private String s201Xml;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws IOException {
        // First read a valid S-124 content to generate the pub-sub message for.
        InputStream inS124 = new ClassPathResource("s124-msg.xml").getInputStream();
        this.s124Xml = IOUtils.toString(inS124, StandardCharsets.UTF_8);
        // Do the same for a valid S-201 content as well
        InputStream inS125 = new ClassPathResource("s125-msg.xml").getInputStream();
        this.s125Xml = IOUtils.toString(inS125, StandardCharsets.UTF_8);
        // Do the same for a valid S-201 content as well
        InputStream inS201 = new ClassPathResource("s125-msg.xml").getInputStream();
        this.s201Xml = IOUtils.toString(inS201, StandardCharsets.UTF_8);
    }

    /**
     * Test that we can publish a Navigation Warning to the application's
     * Navigation Warning publish subscribe channel by accessing the
     * "publish/atons/{atonUID}" endpoint.
     */
    @Test
    void testPublishS124() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/publish/s124/{nwUID}", "NW-01-002")
                        .contentType("application/gml+xml;charset=UTF-8")
                        .param("geometry", "53.61, 1.594, 53.61, 1.594, 53.61, 1.594")
                        .content(this.s124Xml))
                .andExpect(status().isOk())
                .andReturn();

        // Make sure the S125 XML body is also returned for validation
        assertEquals(this.s124Xml, mvcResult.getResponse().getContentAsString());
    }

    /**
     * Test that when trying to publish a Navigation Warning to the application's
     * Navigation Warning publish subscribe channel, if no valid S-125 XML is
     *  provided, an HTTP Bad Response (400) will be returned.
     */
    @Test
    void testPublishS124BadRequest() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/publish/s124/{nwUID}", "NW-01-002")
                        .contentType("application/gml+xml;charset=UTF-8")
                        .param("geometry", "53.61, 1.594, 53.61, 1.594, 53.61, 1.594"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test that we can publish an NW UID to the application's Navigation
     * Warning delete subscribe channel by accessing the
     * "publish/atons/{atonUID}" endpoint.
     */
    @Test
    void testDeleteS124() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(delete("/publish/s124/{nwUID}", "NW-01-002"))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that we can publish an AtoN to the application's AtoN publish
     * subscribe channel by accessing the "publish/atons/{atonUID}" endpoint.
     */
    @Test
    void testPublishS125() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/publish/s125/{atonUID}", "aton.uk.test_aton")
                .contentType("application/gml+xml;charset=UTF-8")
                .param("geometry", "53.61, 1.594")
                .content(this.s125Xml))
                .andExpect(status().isOk())
                .andReturn();

        // Make sure the S125 XML body is also returned for validation
        assertEquals(this.s125Xml, mvcResult.getResponse().getContentAsString());
    }

    /**
     * Test that when trying to publish an AtoN to the application's AtoN
     * publish subscribe channel, if no valid S-125 XML is provided, an HTTP
     * Bad Response (400) will be returned.
     */
    @Test
    void testPublishS125BadRequest() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/publish/s125/{atonUID}", "aton.uk.test_aton")
                .contentType("application/gml+xml;charset=UTF-8")
                .param("geometry", "53.61, 1.594"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test that we can publish an AtoN UID to the application's AtoN delete
     * subscribe channel by accessing the "publish/atons/{atonUID}" endpoint.
     */
    @Test
    void testDeleteS125() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(delete("/publish/s125/{atonUID}", "aton.uk.test_aton"))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that we can publish an AtoN to the application's Admin AtoN publish
     * subscribe channel by accessing the "publish/s201/{atonUID}" endpoint.
     */
    @Test
    void testPublishS201() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/publish/s201/{atonUID}", "aton.uk.test_aton")
                        .contentType("application/gml+xml;charset=UTF-8")
                        .param("geometry", "53.61, 1.594")
                        .content(this.s201Xml))
                .andExpect(status().isOk())
                .andReturn();

        // Make sure the S125 XML body is also returned for validation
        assertEquals(this.s201Xml, mvcResult.getResponse().getContentAsString());
    }

    /**
     * Test that when trying to publish an AtoN to the application's Admin AtoN
     * publish subscribe channel, if no valid S-201 XML is provided, an HTTP
     * Bad Response (400) will be returned.
     */
    @Test
    void testPublishS201BadRequest() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/publish/s201/{atonUID}", "aton.uk.test_aton")
                        .contentType("application/gml+xml;charset=UTF-8")
                        .param("geometry", "53.61, 1.594"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test that we can publish an AtoN UID to the application's Admin AtoN
     * delete subscribe channel by accessing the "publish/atons/{atonUID}"
     * endpoint.
     */
    @Test
    void testDeleteS201() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(delete("/publish/s201/{atonUID}", "aton.uk.test_aton"))
                .andExpect(status().isOk())
                .andReturn();
    }

}