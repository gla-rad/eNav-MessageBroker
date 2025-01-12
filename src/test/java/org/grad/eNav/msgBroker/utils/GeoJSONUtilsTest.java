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

package org.grad.eNav.msgBroker.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class GeoJSONUtilsTest {

    /**
     * Test that we can create the GeoJSON point definitions correctly for any
     * given x and y coordinates.
     */
    @Test
    public void testCreateGeoJSONPoint() {
        JsonNode point00 = GeoJSONUtils.createGeoJSON(0, 0);
        assertNotNull(point00);
        assertEquals("Point", point00.get("type").textValue());
        assertEquals("[0.0,0.0]", point00.get("coordinates").toString());
        assertEquals("EPSG:4326", point00.get("crs").get("properties").get("name").textValue());

        JsonNode point12 = GeoJSONUtils.createGeoJSON(1, 2);
        assertNotNull(point12);
        assertEquals("Point", point12.get("type").textValue());
        assertEquals("[1,2]", point12.get("coordinates").toString());
        assertEquals("EPSG:4326", point12.get("crs").get("properties").get("name").textValue());

        JsonNode point18090 = GeoJSONUtils.createGeoJSON(180, 90);
        assertNotNull(point18090);
        assertEquals("Point", point18090.get("type").textValue());
        assertEquals("[180,90]", point18090.get("coordinates").toString());
        assertEquals("EPSG:4326", point18090.get("crs").get("properties").get("name").textValue());

        JsonNode point18090CRS = GeoJSONUtils.createGeoJSON(180, 90, 2810);
        assertNotNull(point18090CRS);
        assertEquals("Point", point18090CRS.get("type").textValue());
        assertEquals("[180,90]", point18090CRS.get("coordinates").toString());
        assertEquals("EPSG:2810", point18090CRS.get("crs").get("properties").get("name").textValue());
    }

    /**
     * Test that we can create the GeoJSON line string definitions correctly for
     * any given list of x and y coordinate pairs.
     */
    @Test
    public void testCreateGeoJSONLineString() {
        JsonNode point00_1 = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{0.0, 0.0, 0.0, 0.0}).toList());
        assertNotNull(point00_1);
        assertEquals("LineString", point00_1.get("type").textValue());
        assertEquals("[[0.0,0.0],[0.0,0.0]]", point00_1.get("coordinates").toString());
        assertEquals("EPSG:4326", point00_1.get("crs").get("properties").get("name").textValue());

        JsonNode point00_2 = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{-1.0, -2.0, 1.0, 2.0}).toList());
        assertNotNull(point00_2);
        assertEquals("LineString", point00_2.get("type").textValue());
        assertEquals("[[-1,-2],[1,2]]", point00_2.get("coordinates").toString());
        assertEquals("EPSG:4326", point00_2.get("crs").get("properties").get("name").textValue());

        JsonNode point00_3 = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{-1.0, -2.0, 1.0, 2.0}).toList(), 2810);
        assertNotNull(point00_3);
        assertEquals("LineString", point00_3.get("type").textValue());
        assertEquals("[[-1,-2],[1,2]]", point00_3.get("coordinates").toString());
        assertEquals("EPSG:2810", point00_3.get("crs").get("properties").get("name").textValue());

        JsonNode point00_13 = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0}).toList());
        assertNotNull(point00_1);
        assertEquals("LineString", point00_13.get("type").textValue());
        assertEquals("[[0.0,0.0],[0.0,0.0],[0.0,0.0]]", point00_13.get("coordinates").toString());
        assertEquals("EPSG:4326", point00_13.get("crs").get("properties").get("name").textValue());

        JsonNode point00_23 = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{-1.0, -2.0, 1.0, 2.0, -1.0, -2.0}).toList());
        assertNotNull(point00_2);
        assertEquals("LineString", point00_23.get("type").textValue());
        assertEquals("[[-1,-2],[1,2],[-1,-2]]", point00_23.get("coordinates").toString());
        assertEquals("EPSG:4326", point00_23.get("crs").get("properties").get("name").textValue());

        JsonNode point00_33 = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{-1.0, -2.0, 1.0, 2.0, -1.0, -2.0}).toList(), 2810);
        assertNotNull(point00_3);
        assertEquals("LineString", point00_33.get("type").textValue());
        assertEquals("[[-1,-2],[1,2],[-1,-2]]", point00_33.get("coordinates").toString());
        assertEquals("EPSG:2810", point00_33.get("crs").get("properties").get("name").textValue());
    }

    /**
     * Test that we can create the GeoJSON polygon definitions correctly for
     * any given list of x and y coordinates.
     */
    @Test
    public void testCreateGeoJSONPolygon() {
        JsonNode polygon00000 = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}).toList());
        assertNotNull(polygon00000);
        assertEquals("Polygon", polygon00000.get("type").textValue());
        assertEquals("[[[0.0,0.0],[0.0,0.0],[0.0,0.0],[0.0,0.0]]]", polygon00000.get("coordinates").toString());
        assertEquals("EPSG:4326", polygon00000.get("crs").get("properties").get("name").textValue());

        JsonNode square12 = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{1.0, 2.0, 1.0, -2.0, -1.0, -2.0, -1.0, 2.0, 1.0, 2.0}).toList());
        assertNotNull(square12);
        assertEquals("Polygon", square12.get("type").textValue());
        assertEquals("[[[1,2],[1,-2],[-1,-2],[-1,2],[1,2]]]", square12.get("coordinates").toString());
        assertEquals("EPSG:4326", square12.get("crs").get("properties").get("name").textValue());

        JsonNode square12CRS = GeoJSONUtils.createGeoJSON(Arrays.stream(new Double[]{1.0, 2.0, 1.0, -2.0, -1.0, -2.0, -1.0, 2.0, 1.0, 2.0}).toList(), 2810);
        assertNotNull(square12CRS);
        assertEquals("Polygon", square12CRS.get("type").textValue());
        assertEquals("[[[1,2],[1,-2],[-1,-2],[-1,2],[1,2]]]", square12CRS.get("coordinates").toString());
        assertEquals("EPSG:2810", square12CRS.get("crs").get("properties").get("name").textValue());
    }

}