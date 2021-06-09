package org.grad.eNav.msgBroker.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * The GeoJSON Utils Class.
 *
 * This is a utility class that allows the easy manipulation of the GeoJSON
 * data. For example, we want to easily create GeoJSON point from x and y
 * coordinates.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class GeoJSONUtils {

    /**
     * A helper function that will return a GeoJSON Point object based on the
     * provided x and y coordinates.
     *
     * @param x the x value of the coordinate
     * @param y the y value of the coordinate
     * @return the GeoJSON point object
     */
    public static JsonNode createGeoJSONPoint(double x, double y) {
        return createGeoJSONPoint(x, y, null);
    }

    /**
     * A helper function that will return a GeoJSON Point object based on the
     * provided x and y coordinates. This extended version also allows users
     * to define the coordinate reference system through the SRID parameter.
     *
     * @param x the x value of the coordinate
     * @param y the y value of the coordinate
     * @param srid the default coordinate reference system ID - defaults to EPSG:4326
     * @return the GeoJSON point object
     */
    public static JsonNode createGeoJSONPoint(double x, double y, Integer srid) {
        // First create a com.locationtech.jts Point geometry;
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), Optional.ofNullable(srid).orElse(4326));
        Point point = factory.createPoint(new Coordinate(x, y));

        // Now convert into a JSON node
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readTree(new GeoJsonWriter().write(point));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * To easily translate a GeoJSON point to an ECQL point description, this
     * utility function reads the json node and if a valid point
     *
     * @param point the GeoJSON point to be translated
     * @return The ECQL description of the point
     */
    public static String geoJSONPointToECQL(JsonNode point) {
        // Sanity check
        if(Objects.isNull(point)) {
            return "";
        }
        JsonNode type = point.get("type");
        JsonNode coordinates = point.get("coordinates");
        if(Objects.nonNull(type) && type.asText().equals("Point") && Objects.nonNull(coordinates) && coordinates.isArray()) {
            return String.format("POINT (%s %s)", coordinates.get(0).toString(), coordinates.get(1).toString());
        }
        else {
            return "";
        }
    }

}
