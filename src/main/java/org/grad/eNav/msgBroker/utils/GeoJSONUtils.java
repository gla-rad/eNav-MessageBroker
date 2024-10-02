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
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

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
     * <p/>
     * Note that in the default coordinate system SRS 4326, the x-y coordinates
     * should them be mapped to the lat-lon order.
     *
     * @param x the x value of the coordinate
     * @param y the y value of the coordinate
     * @return the GeoJSON point object
     */
    public static JsonNode createGeoJSON(double x, double y) {
        return createGeoJSON(x, y, null);
    }

    /**
     * A helper function that will return a GeoJSON Point object based on the
     * geometry centroid for the provided list of x and y coordinates.
     * <p/>
     * Note that in the default coordinate system SRS 4326, the x-y coordinates
     * should them be mapped to the lat-lon order.
     *
     * @param xy the list of xy coordinates to create the polygon from
     * @return the GeoJSON point object
     */
    public static JsonNode createGeoJSON(List<Double> xy) {
        return createGeoJSON(xy, null);
    }

    /**
     * A helper function that will return a GeoJSON Point object based on the
     * provided x and y coordinates. This extended version also allows users
     * to define the coordinate reference system through the SRID parameter.
     * <p/>
     * Note that in the default coordinate system SRS 4326, the x-y coordinates
     * should them be mapped to the lat-lon order.
     *
     * @param x the x value of the coordinate
     * @param srid the default coordinate reference system ID - defaults to EPSG:4326
     * @return the GeoJSON point object
     */
    public static JsonNode createGeoJSON(double x, double y, Integer srid) {
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
     * A helper function that will return a GeoJSON Point object based on the
     * provided list of x and y coordinates. The list of coordinates is expected
     * to form a geometry, for which the centroid point will be used to define
     * the final point. This extended version also allows users to define the
     * coordinate reference system through the SRID parameter.
     * <p/>
     * Note that in the default coordinate system SRS 4326, the x-y coordinates
     * should them be mapped to the lat-lon order.
     *
     * @param xy a list of xy coordinates to create the polygon from
     * @param srid the default coordinate reference system ID - defaults to EPSG:4326
     * @return the GeoJSON point object
     */
    public static JsonNode createGeoJSON(List<Double> xy, Integer srid) {
        // Collect the coordinates to an array
        Coordinate[] coordinates = IntStream.range(0, xy.size()/2)
                .mapToObj(i -> new Coordinate(xy.get(i*2), xy.get(i*2+1)))
                .toList()
                .toArray(new Coordinate[]{});

        // First create a geometry factory
        GeometryFactory factory = new GeometryFactory(
                new PrecisionModel(),
                Optional.ofNullable(srid).orElse(4326));

        // The select the appropriate geometry for the input type
        String geometryType;
        if(coordinates.length <= 1) {
            geometryType = "point";
        } else if(coordinates.length <= 3 || !coordinates[0].equals2D(coordinates[coordinates.length-1])) {
            geometryType = "linestring";
        } else {
            geometryType = "polygon";
        }

        // And generate the matching geometry
        Geometry geometry = switch (geometryType) {
            case "point" -> factory.createPoint(coordinates[0]);
            case "linestring" -> factory.createLineString(coordinates);
            case "polygon" -> factory.createPolygon(coordinates);
            default -> null;
        };

        // Finally convert into a JSON node
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readTree(new GeoJsonWriter().write(geometry));
        } catch (IOException e) {
            return null;
        }
    }

}
