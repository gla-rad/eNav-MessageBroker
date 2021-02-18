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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Used for serializing and de-serializing an AtonTag array as Maps.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class AtonTagJsonSerialization {

    /**
     * Serializes an AtonTagVo array as a Map
     */
    public static class Serializer extends JsonSerializer<AtonTag[]> {

        /** {@inheritDoc} */
        @Override
        public void serialize(AtonTag[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            // NB: We collect to a linked hash map to preserve the order of the tags
            Map<String, String> tags = Arrays.stream(value)
                    .collect(Collectors.toMap(AtonTag::getK, AtonTag::getV, (t1, t2) -> t1, LinkedHashMap::new));
            gen.writeObject(tags);
        }
    }

    /**
     * De-serializes a Map as an AtonTagVo array
     */
    public static class Deserializer extends JsonDeserializer<AtonTag[]> {

        /** {@inheritDoc} */
        @Override
        public AtonTag[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            List<AtonTag> tags = new ArrayList<>();

            if (node != null) {
                for (Iterator<String> i = node.fieldNames(); i.hasNext(); ) {
                    String k = i.next();
                    String v = node.get(k).asText();
                    tags.add(new AtonTag(k, v));
                }
            }
            return  tags.toArray(new AtonTag[tags.size()]);
        }
    }
}
