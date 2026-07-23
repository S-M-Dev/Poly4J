package pro.smdev.poly4j.mapper;

/*
 * Copyright 2026 S-M-Dev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.net.http.HttpResponse;

/**
 * Mapper from HttpResponse&lt;String&gt; to {@link JsonNode}
 *
 * @author ALazyGuy
 * @since 1.0.0
 * @see ResponseMapper
 */
public class JsonResponseMapper implements ResponseMapper<JsonNode> {

    /**
     * Map String response to {@link JsonNode}
     * @param response HttpResponse
     * @return parsed json as {@link JsonNode} and empty {@link com.fasterxml.jackson.databind.node.TextNode} on exception
     */
    @Override
    public JsonNode map(HttpResponse<String> response) {
        try {
            return new JsonMapper().readTree(response.body());
        } catch (JsonProcessingException e) {
            return new JsonMapper().getNodeFactory().textNode(response.body());
        }
    }
}
