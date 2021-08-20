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

package org.grad.eNav.msgBroker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The AtonListenerProperties Class
 *
 * This class implements the mapping of the configuration properties onto a
 * POJO that can be used inside the app. The properties are defined dynamically
 * through an array and a custom number of endpoints can be defined.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
@ConfigurationProperties("gla.rad.msg-broker.aton")
public class AtonListenerProperties {

    // Class Variables
    private List<Listener> listeners = new ArrayList<>();

    /**
     * Gets listeners.
     *
     * @return Value of listeners.
     */
    public List<Listener> getListeners() {
        return listeners;
    }

    /**
     * Sets new listeners.
     *
     * @param listeners New value of listeners.
     */
    public void setListeners(List<Listener> listeners) {
        this.listeners = listeners;
    }

    /**
     * A custom configuration class to describe a single listener configuration.
     */
    public static class Listener {

        // Class Variables
        private String address;
        private int port;
        private List<Double> polygon;

        /**
         * Sets new polygon.
         *
         * @param polygon New value of polygon.
         */
        public void setPolygon(List<Double> polygon) {
            this.polygon = polygon;
        }

        /**
         * Sets new port.
         *
         * @param port New value of port.
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * Gets port.
         *
         * @return Value of port.
         */
        public int getPort() {
            return port;
        }

        /**
         * Sets new address.
         *
         * @param address New value of address.
         */
        public void setAddress(String address) {
            this.address = address;
        }

        /**
         * Gets address.
         *
         * @return Value of address.
         */
        public String getAddress() {
            return address;
        }

        /**
         * Gets polygon.
         *
         * @return Value of polygon.
         */
        public List<Double> getPolygon() {
            return polygon;
        }

        /**
         * Overriding the toString() method to prettify the output.
         *
         * @return The String representation of a Listener object
         */
        @Override
        public String toString() {
            return "Listener{" +
                    "address='" + address + '\'' +
                    ", port=" + port +
                    ", polygon=" + polygon +
                    '}';
        }
    }

}
