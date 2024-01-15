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

package org.grad.eNav.msgBroker.models;

/**
 * The Publication Type Enum.
 *
 * This enumeration is used to define all the publication types currently
 * supported by the message broker micro-service.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public enum PublicationType {
    ATON("aton"),
    ATON_DEL("aton-delete"),
    NAVIGATION_MESSAGE("navigation-message"),
    NAVIGATION_MESSAGE_DEL("navigation-message-delete"),
    NAVIGATION_WARNING("navigation-warning"),
    NAVIGATION_WARNING_DEL("navigation-warning-delete");

    /**
     * The Publication Type string representation.
     */
    private String type;

    /**
     * The Publication Type Constructor.
     *
     * @param type      The string representation of the type
     */
    PublicationType(String type) {
        this.type = type;
    }

    /**
     * Gets type.
     *
     * @return Value of type.
     */
    public String getType() {
        return type;
    }
}
