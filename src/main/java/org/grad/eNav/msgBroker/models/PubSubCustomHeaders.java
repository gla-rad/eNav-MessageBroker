/*
 * Copyright (c) 2021 GLA UK Research and Development Directive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.grad.eNav.msgBroker.models;

/**
 * The PubSubCustomHeaders Class
 *
 * This is a class that defines the additional custom headers for the S-124 and
 * S-125 custom headers of the publish subscribe internal messages of the
 * broker micro-service.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class PubSubCustomHeaders {

    // The Custom Headers
    public static String PUBSUB_S124_ID = "PUBSUB-S124-ID";
    public static String PUBSUB_S125_ID = "PUBSUBS-125-ID";
    public static String PUBSUB_BBOX = "PUBSUB-BBOX";

}
