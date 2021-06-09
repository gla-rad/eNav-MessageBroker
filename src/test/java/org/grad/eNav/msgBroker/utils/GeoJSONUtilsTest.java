package org.grad.eNav.msgBroker.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GeoJSONUtilsTest {

    /**
     * Test that we can create the GeoJSON point definitions correctly for the
     * any given x and y coordinates.
     */
    @Test
    public void testCreateGeoJSONPoint() {
        JsonNode point00 = GeoJSONUtils.createGeoJSONPoint(0, 0);
        assertNotNull(point00);
        assertEquals("Point", point00.get("type").textValue());
        assertEquals("[0.0,0.0]", point00.get("coordinates").toString());

        JsonNode point12 = GeoJSONUtils.createGeoJSONPoint(1, 2);
        assertNotNull(point12);
        assertEquals("Point", point12.get("type").textValue());
        assertEquals("[1,2]", point12.get("coordinates").toString());

        JsonNode point18090 = GeoJSONUtils.createGeoJSONPoint(180, 90);
        assertNotNull(point18090);
        assertEquals("Point", point18090.get("type").textValue());
        assertEquals("[180,90]", point18090.get("coordinates").toString());
    }

    /**
     * Test that we can translate the GeoJSON points correctly to their ECQL
     * descriptions.
     */
    @Test
    public void testGeoJSONPointToECQL() {
        String pointNull = GeoJSONUtils.geoJSONPointToECQL(null);
        assertEquals("", pointNull);

        String pointEmpty = GeoJSONUtils.geoJSONPointToECQL(new ObjectMapper().createObjectNode());
        assertEquals("", pointEmpty);

        JsonNode point00 = GeoJSONUtils.createGeoJSONPoint(0, 0);
        String point00ECQL = GeoJSONUtils.geoJSONPointToECQL(point00);
        assertEquals("POINT (0.0 0.0)", point00ECQL);

        JsonNode point12 = GeoJSONUtils.createGeoJSONPoint(1, 2);
        String point12ECQL = GeoJSONUtils.geoJSONPointToECQL(point12);
        assertEquals("POINT (1 2)", point12ECQL);

        JsonNode point18090 = GeoJSONUtils.createGeoJSONPoint(180, 90);
        String point18090ECQL = GeoJSONUtils.geoJSONPointToECQL(point18090);
        assertEquals("POINT (180 90)", point18090ECQL);
    }

}